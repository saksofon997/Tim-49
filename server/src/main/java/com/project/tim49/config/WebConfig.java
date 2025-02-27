package com.project.tim49.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:4200");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry
                .addResourceHandler("/assets/images/**")
                .setCachePeriod(0)
                .addResourceLocations("classpath:/static/assets/images/");
    }
}
