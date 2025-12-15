package com.zeezaglobal.prescription.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://147.93.114.66:8081",
                        "http://localhost:3000",
                        "http://127.0.0.1:9090",
                        "https://indigorx.me"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")  // This is crucial for preflight requests
                .allowCredentials(true)  // If you're using cookies/auth tokens
                .maxAge(3600);  // Cache preflight response for 1 hour
    }
}