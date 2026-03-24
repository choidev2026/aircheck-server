package com.seriouschoi.aircheck.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Firebase App Check 토큰 검증 필터
 * 
 * 현재: 로그만 (통과 모드)
 * TODO: Firebase Admin SDK 연동 후 검증 활성화
 */
@Component
class AppCheckFilter : OncePerRequestFilter() {

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
        } else {
            log.debug("[AppCheck] 토큰 수신: {} {} (length={})", request.method, path, token.length)
            // TODO: Firebase Admin SDK로 토큰 검증
            // verifyToken(token)
        }
        
        // 현재는 항상 통과 (로그만)
        filterChain.doFilter(request, response)
    }
    
    private fun isExcludedPath(path: String): Boolean {
        return EXCLUDED_PATHS.any { path.startsWith(it) }
    }
    
    // TODO: Firebase Admin SDK 연동 후 구현
    // private fun verifyToken(token: String): Boolean {
    //     return try {
    //         FirebaseAppCheck.getInstance().verifyToken(token)
    //         true
    //     } catch (e: Exception) {
    //         log.warn("[AppCheck] 토큰 검증 실패: {}", e.message)
    //         false
    //     }
    // }
}
