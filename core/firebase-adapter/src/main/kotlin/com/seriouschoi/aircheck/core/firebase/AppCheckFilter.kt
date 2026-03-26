package com.seriouschoi.aircheck.core.firebase

import com.google.firebase.FirebaseApp
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

/**
 * Firebase App Check 토큰 검증 필터
 * 
 * appcheck.enabled=true 일 때만 차단 모드
 * false일 때는 로그만 찍고 통과
 */
@Component
class AppCheckFilter(
    @Value("\${appcheck.enabled:false}")
    private val enabled: Boolean,
    @Value("\${firebase.project-id:}")
    private val projectId: String
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val HEADER_APP_CHECK = "X-Firebase-AppCheck"
        
        // 검증 제외 경로
        private val EXCLUDED_PATHS = listOf(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/admin"
        )
    }
    
    init {
        log.info("[AppCheck] 필터 초기화 (enabled={}, projectId={})", enabled, projectId)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        
        // 제외 경로는 검증 스킵
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response)
            return
        }
        
        val token = request.getHeader(HEADER_APP_CHECK)
        
        if (token.isNullOrBlank()) {
            log.warn("[AppCheck] 토큰 없음: {} {}", request.method, path)
            if (enabled) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "App Check token required")
                return
            }
        } else {
            val isValid = verifyToken(token)
            if (isValid) {
                log.debug("[AppCheck] 토큰 유효: {} {}", request.method, path)
            } else {
                log.warn("[AppCheck] 토큰 무효: {} {}", request.method, path)
                if (enabled) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid App Check token")
                    return
                }
            }
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun isExcludedPath(path: String): Boolean {
        return EXCLUDED_PATHS.any { path.startsWith(it) }
    }
    
    /**
     * App Check 토큰 검증
     * 
     * JWT 형식 검증 + issuer/audience 확인
     * TODO: Firebase 공개키로 서명 검증 추가
     */
    private fun verifyToken(token: String): Boolean {
        return try {
            // JWT 형식 확인 (header.payload.signature)
            val parts = token.split(".")
            if (parts.size != 3) {
                log.warn("[AppCheck] JWT 형식 아님")
                return false
            }
            
            // Payload 디코딩
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            
            // issuer 확인 (Firebase App Check)
            if (!payload.contains("\"iss\":\"https://firebaseappcheck.googleapis.com/")) {
                log.warn("[AppCheck] issuer 불일치")
                return false
            }
            
            // audience 확인 (프로젝트 ID)
            if (projectId.isNotBlank() && !payload.contains("\"aud\":[\"projects/$projectId\"]")) {
                // audience 형식이 다를 수 있으므로 프로젝트 ID만 포함 여부 확인
                if (!payload.contains(projectId)) {
                    log.warn("[AppCheck] audience 불일치")
                    return false
                }
            }
            
            // 만료 시간 확인
            val expMatch = Regex("\"exp\":(\\d+)").find(payload)
            if (expMatch != null) {
                val exp = expMatch.groupValues[1].toLong()
                val now = System.currentTimeMillis() / 1000
                if (exp < now) {
                    log.warn("[AppCheck] 토큰 만료")
                    return false
                }
            }
            
            true
        } catch (e: Exception) {
            log.warn("[AppCheck] 검증 실패: {}", e.message)
            false
        }
    }
}
