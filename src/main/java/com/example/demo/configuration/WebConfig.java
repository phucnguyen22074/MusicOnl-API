package com.example.demo.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Phục vụ file audio từ thư mục bên ngoài
        registry.addResourceHandler("/api/audio/**")
                .addResourceLocations("file:./audio-storage/")
                .setCachePeriod(3600); // Cache 1 giờ

        // Phục vụ file static mặc định
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}