package com.zeezaglobal.prescription.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow CORS for React app on localhost:3000
        registry.addMapping("/**")
                .allowedOrigins("http://147.93.114.66:8081", "http://localhost:3000", "http://127.0.0.1:9090","https://indigorx.me")  // Add your React app URL
                .allowedMethods("GET", "POST", "DELETE");
    }
}
