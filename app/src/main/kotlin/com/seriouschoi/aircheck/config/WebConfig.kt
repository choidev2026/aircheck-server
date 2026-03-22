package com.seriouschoi.aircheck.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Admin 페이지 정적 리소스 설정
 */
@Configuration
class WebConfig : WebMvcConfigurer {
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // /admin/** 요청을 static/admin/에서 서빙
        registry.addResourceHandler("/admin/**")
            .addResourceLocations("classpath:/static/admin/")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // /admin 요청 시 index.html로 포워드 (SPA 라우팅)
        registry.addViewController("/admin").setViewName("forward:/admin/index.html")
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html")
    }
}
