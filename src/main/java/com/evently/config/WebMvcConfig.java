package com.evently.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.evently.interceptor.AdminInterceptor;

/**
 * Spring MVC configuration.
 *
 * Registers the AdminInterceptor for all /admin/** paths so that any request
 * to the admin area that somehow bypasses Spring Security (e.g. misconfigured
 * path patterns) is still rejected.
 *
 * OWNER: Faris (registers interceptor)
 * INTERCEPTOR IMPL: Mohamed Morsy (AdminInterceptor.java)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Double-check: reject non-admin users even if a route accidentally slips through Security config.
        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**");
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
    }
}
