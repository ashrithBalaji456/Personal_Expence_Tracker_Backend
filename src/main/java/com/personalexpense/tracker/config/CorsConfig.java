package com.personalexpense.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:5173",
                "http://localhost:8080",
                "https://*.onrender.com",
                "https://*.netlify.app",
                "https://*.vercel.app"
        ));
        
        config.setAllowedHeaders(Arrays.asList(
                "Origin", 
                "Access-Control-Allow-Origin", 
                "Content-Type", 
                "Accept", 
                "Authorization", 
                "Origin, Accept", 
                "X-Requested-With", 
                "Access-Control-Request-Method", 
                "Access-Control-Request-Headers"
        ));
        
        config.setExposedHeaders(Arrays.asList(
                "Origin", 
                "Content-Type", 
                "Accept", 
                "Authorization", 
                "Access-Control-Allow-Origin", 
                "Access-Control-Allow-Credentials"
        ));
        
        config.setAllowedMethods(Arrays.asList(
                "GET", 
                "POST", 
                "PUT", 
                "DELETE", 
                "OPTIONS", 
                "PATCH"
        ));
        
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
