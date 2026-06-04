package com.ecommerce.config;

import com.ecommerce.security.AdminInterceptor;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.security.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${upload.path:./upload}")
    private String uploadPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(jwtUtils))
                .addPathPatterns("/api/upload", "/api/reviews/**",
                        "/api/cart/**", "/api/user/**", "/api/orders/**",
                        "/api/favorites/**", "/api/coupons/*/receive", "/api/user/coupons/**",
                        "/api/conversations/**", "/api/seckill/order",
                        "/api/feedback/**");

        registry.addInterceptor(new AdminInterceptor(jwtUtils))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
