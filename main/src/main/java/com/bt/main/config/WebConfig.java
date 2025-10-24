package com.bt.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources from the static directory
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // Serve frontend assets directly from root
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);

        // Serve other static files (favicon, etc.)
        registry.addResourceHandler("/favicon.ico", "/vite.svg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}
