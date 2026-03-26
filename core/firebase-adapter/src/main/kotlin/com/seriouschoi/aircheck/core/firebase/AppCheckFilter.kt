package com.seriouschoi.aircheck.core.firebase

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

/**
 * Firebase App Check 토큰 검증 필터
 * 
 * - JWKS에서 공개키를 가져와 JWT 서명 검증
 * - issuer, audience, 만료시간 확인
 * - appcheck.enabled=true 일 때만 차단 모드
 */
@Component
class AppCheckFilter(
    @Value("\${appcheck.enabled:false}")
    private val enabled: Boolean,
    @Value("\${firebase.project-id:}")
    private val projectId: String,
    @Value("\${firebase.project-number:}")
    private val projectNumber: String
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val HEADER_APP_CHECK = "X-Firebase-AppCheck"
        private const val JWKS_URL = "https://firebaseappcheck.googleapis.com/v1/jwks"
        private const val ISSUER_PREFIX = "https://firebaseappcheck.googleapis.com/"
        
        // 검증 제외 경로
        private val EXCLUDED_PATHS = listOf(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/admin"
        )
    }
    
    // JWKS Provider (캐싱 + 자동 갱신)
    private val jwkProvider = JwkProviderBuilder(URL(JWKS_URL))
        .cached(10, 24, TimeUnit.HOURS)  // 최대 10개 키, 24시간 캐시
        .rateLimited(10, 1, TimeUnit.MINUTES)  // 분당 10회 제한
        .build()
    
    init {
        log.info("[AppCheck] 필터 초기화 (enabled={}, projectNumber={})", enabled, projectNumber)
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
     * 1. JWT 디코딩
     * 2. JWKS에서 공개키로 서명 검증
     * 3. issuer, audience, 만료시간 확인
     */
    private fun verifyToken(token: String): Boolean {
        return try {
            // JWT 디코딩 (검증 전)
            val jwt = JWT.decode(token)
            
            // JWKS에서 공개키 가져오기
            val jwk = jwkProvider.get(jwt.keyId)
            val publicKey = jwk.publicKey as RSAPublicKey
            
            // 알고리즘 설정 및 검증
            val algorithm = Algorithm.RSA256(publicKey, null)
            val verifier = JWT.require(algorithm)
                .acceptLeeway(60)  // 60초 여유
                .build()
            
            // 서명 검증
            val verified: DecodedJWT = verifier.verify(token)
            
            // issuer 확인 (정확히 일치)
            val issuer = verified.issuer ?: ""
            val expectedIssuer = "$ISSUER_PREFIX$projectNumber"
            if (projectNumber.isNotBlank() && issuer != expectedIssuer) {
                log.warn("[AppCheck] issuer 불일치: expected={}, actual={}", expectedIssuer, issuer)
                return false
            }
            
            // audience 확인 (프로젝트 ID 포함 여부)
            val audiences = verified.audience ?: emptyList()
            if (projectId.isNotBlank() && audiences.none { it.contains(projectId) }) {
                log.warn("[AppCheck] audience 불일치: {}", audiences)
                return false
            }
            
            log.debug("[AppCheck] 검증 성공: iss={}, sub={}", issuer, verified.subject)
            true
        } catch (e: Exception) {
            log.warn("[AppCheck] 검증 실패: {}", e.message)
            false
        }
    }
}
