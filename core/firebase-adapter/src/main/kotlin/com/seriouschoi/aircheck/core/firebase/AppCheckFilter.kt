package com.seriouschoi.aircheck.core.firebase

import com.google.firebase.FirebaseApp
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Firebase App Check 토큰 검증 필터
 * 
 * appcheck.enabled=true 일 때만 차단 모드
 * false일 때는 로그만 찍고 통과
 */
@Component
class AppCheckFilter(
    @Value("\${appcheck.enabled:false}")
    private val enabled: Boolean
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
        log.info("[AppCheck] 필터 초기화 (enabled={})", enabled)
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
            log.info("[AppCheck] 토큰 수신: {} {} (length={})", request.method, path, token.length)
            
            // TODO: Firebase Admin SDK로 토큰 검증 구현
            // 현재는 토큰이 있으면 통과
            // val isValid = verifyToken(token)
            // if (!isValid && enabled) {
            //     response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid App Check token")
            //     return
            // }
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun isExcludedPath(path: String): Boolean {
        return EXCLUDED_PATHS.any { path.startsWith(it) }
    }
}
