package com.alibaba.example.chatmemory.mem0;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemZeroConfig {

    @Value("${mem0.api.base-url:http://localhost:8888}")
    private String baseUrl;
    
    @Value("${mem0.enable-cache:true}")
    private boolean enableCache;
    
    @Value("${mem0.timeout.seconds:30}")
    private int timeoutSeconds;
    
    @Value("${mem0.retry.max-attempts:3}")
    private int maxRetryAttempts;

    public String getBaseUrl() { 
        return baseUrl; 
    }

    public boolean isEnableCache() {
        return enableCache;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
}
