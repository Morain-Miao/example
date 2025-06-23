package com.alibaba.example.chatmemory.mem0;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mem0 API 客户端实现
 * 
 * 直接调用 Mem0 REST API 接口
 * 参考文档: http://localhost:8888/docs
 */
@Component
public class MemZeroHttpClient {

    private static final Logger logger = Logger.getLogger(MemZeroHttpClient.class.getName());
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MemZeroConfig config;
    
    // Mem0 API 端点
    private static final String CONFIGURE_ENDPOINT = "/configure";
    private static final String MEMORIES_ENDPOINT = "/memories";
    private static final String SEARCH_ENDPOINT = "/search";
    private static final String RESET_ENDPOINT = "/reset";
    private static final String HEALTH_ENDPOINT = "/";

    /**
     * 构造函数
     */
    public MemZeroHttpClient(MemZeroConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        // 创建 WebClient 连接到 Mem0 API
        this.webClient = WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /**
     * 配置 Mem0
     */
    public void configure(Map<String, Object> configMap) {
        try {
            String response = webClient.post()
                .uri(CONFIGURE_ENDPOINT)
                .bodyValue(configMap)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(this.config.getTimeoutSeconds()))
                .block();
            
            logger.info("Mem0 configuration updated successfully");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to configure Mem0: " + e.getMessage(), e);
            throw new RuntimeException("Failed to configure Mem0", e);
        }
    }

    /**
     * 添加记忆
     */
    public Map<String, Object> addMemory(MemZeroRequest.MemoryCreate memoryCreate) {
        try {
            String requestBody = objectMapper.writeValueAsString(memoryCreate);
            
            String response = webClient.post()
                .uri(MEMORIES_ENDPOINT)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            if (response != null) {
                Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                logger.info("Successfully added memory with " + memoryCreate.getMessages().size() + " messages");
                return result;
            }
        } catch (WebClientResponseException e) {
            logger.log(Level.WARNING, "HTTP error adding memory: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to add memory: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add memory: " + e.getMessage(), e);
            throw new RuntimeException("Failed to add memory", e);
        }
        
        return new HashMap<>();
    }

    /**
     * 获取所有记忆
     */
    public List<MemZeroMemory> getAllMemories(String userId, String runId, String agentId) {
        try {
            String response = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(MEMORIES_ENDPOINT);
                    if (userId != null) uriBuilder.queryParam("user_id", userId);
                    if (runId != null) uriBuilder.queryParam("run_id", runId);
                    if (agentId != null) uriBuilder.queryParam("agent_id", agentId);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            if (response != null) {
                List<MemZeroMemory> memories = objectMapper.readValue(response, 
                    new TypeReference<List<MemZeroMemory>>() {});
                logger.info("Retrieved " + memories.size() + " memories");
                return memories;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get memories: " + e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }

    /**
     * 获取单个记忆
     */
    public MemZeroMemory getMemory(String memoryId) {
        try {
            String response = webClient.get()
                .uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            if (response != null) {
                MemZeroMemory memory = objectMapper.readValue(response, MemZeroMemory.class);
                logger.info("Retrieved memory: " + memoryId);
                return memory;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get memory " + memoryId + ": " + e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * 搜索记忆
     */
    public List<MemZeroMemory> searchMemories(MemZeroRequest.SearchRequest searchRequest) {
        try {
            String requestBody = objectMapper.writeValueAsString(searchRequest);
            
            String response = webClient.post()
                .uri(SEARCH_ENDPOINT)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            if (response != null) {
                List<MemZeroMemory> memories = objectMapper.readValue(response, 
                    new TypeReference<List<MemZeroMemory>>() {});
                logger.info("Search returned " + memories.size() + " memories for query: " + searchRequest.getQuery());
                return memories;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to search memories: " + e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }

    /**
     * 更新记忆
     */
    public Map<String, Object> updateMemory(String memoryId, Map<String, Object> updatedMemory) {
        try {
            String requestBody = objectMapper.writeValueAsString(updatedMemory);
            
            String response = webClient.put()
                .uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            if (response != null) {
                Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                logger.info("Successfully updated memory: " + memoryId);
                return result;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to update memory " + memoryId + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to update memory", e);
        }
        
        return new HashMap<>();
    }

    /**
     * 获取记忆历史
     */
    public List<Map<String, Object>> getMemoryHistory(String memoryId) {
        try {
            String response = webClient.get()
                .uri(MEMORIES_ENDPOINT + "/{memoryId}/history", memoryId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            if (response != null) {
                List<Map<String, Object>> history = objectMapper.readValue(response, 
                    new TypeReference<List<Map<String, Object>>>() {});
                logger.info("Retrieved history for memory: " + memoryId);
                return history;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get memory history " + memoryId + ": " + e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }

    /**
     * 删除单个记忆
     */
    public void deleteMemory(String memoryId) {
        try {
            webClient.delete()
                .uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            logger.info("Successfully deleted memory: " + memoryId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete memory " + memoryId + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete memory", e);
        }
    }

    /**
     * 删除所有记忆
     */
    public void deleteAllMemories(String userId, String runId, String agentId) {
        try {
            webClient.delete()
                .uri(uriBuilder -> {
                    uriBuilder.path(MEMORIES_ENDPOINT);
                    if (userId != null) uriBuilder.queryParam("user_id", userId);
                    if (runId != null) uriBuilder.queryParam("run_id", runId);
                    if (agentId != null) uriBuilder.queryParam("agent_id", agentId);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            logger.info("Successfully deleted all memories");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete all memories: " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete all memories", e);
        }
    }

    /**
     * 重置所有记忆
     */
    public void resetAllMemories() {
        try {
            webClient.post()
                .uri(RESET_ENDPOINT)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
            
            logger.info("Successfully reset all memories");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to reset all memories: " + e.getMessage(), e);
            throw new RuntimeException("Failed to reset all memories", e);
        }
    }

    /**
     * 健康检查
     */
    public boolean ping() {
        try {
            String response = webClient.get()
                .uri(HEALTH_ENDPOINT)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            logger.info("Mem0 API is healthy");
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Mem0 API health check failed: " + e.getMessage(), e);
            return false;
        }
    }
} 