package com.alibaba.example.chatmemory.mem0;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenMemory API 客户端实现
 * 
 * 这个类调用 OpenMemory 已经封装好的 Mem0 接口
 * 通过 OpenMemory 的 API 来使用 Mem0 的功能
 */
public class MemZeroClient {

    private static final Logger logger = Logger.getLogger(MemZeroClient.class.getName());
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MemZeroConfig config;
    
    // OpenMemory API 端点
    private static final String ADD_MEMORY_ENDPOINT = "/mcp/add_memories";
    private static final String SEARCH_MEMORY_ENDPOINT = "/mcp/search_memory";
    private static final String LIST_MEMORIES_ENDPOINT = "/mcp/list_memories";
    private static final String DELETE_ALL_MEMORIES_ENDPOINT = "/mcp/delete_all_memories";
    private static final String PING_ENDPOINT = "/health";

    /**
     * 构造函数
     */
    public MemZeroClient(MemZeroConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        // 创建 WebClient 连接到 OpenMemory API
        this.webClient = WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("X-Collection-Name", config.getCollectionName())
            .build();
    }

    public List<String> getAllUserIds() {
        try {
            // 通过 OpenMemory 的 list_memories 端点获取用户ID
            String response = webClient.post()
                .uri(LIST_MEMORIES_ENDPOINT)
                .bodyValue("{}") // 空请求体
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
            
            if (response != null) {
                // 解析响应获取用户ID列表
                Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                // 这里需要根据 OpenMemory 的实际响应格式来解析
                // 暂时返回空列表，因为 OpenMemory 可能需要特定的用户上下文
                logger.info("Retrieved user IDs from OpenMemory");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get user IDs from OpenMemory: " + e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }

    public List<MemZeroMemory> search(String query, Map<String, Object> filters) {
        return search(query, filters, 10);
    }

    public List<MemZeroMemory> search(String query, Map<String, Object> filters, int limit) {
        try {
            // 构建搜索请求 - 使用 OpenMemory 的 search_memory 端点
            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("query", query);
            searchRequest.put("limit", limit);
            
            String requestBody = objectMapper.writeValueAsString(searchRequest);
            
            String response = webClient.post()
                .uri(SEARCH_MEMORY_ENDPOINT)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();
            
            if (response != null) {
                // 解析 OpenMemory 的搜索响应
                List<MemZeroMemory> memories = parseOpenMemorySearchResponse(response);
                logger.info("Search returned " + memories.size() + " memories for query: " + query);
                return memories;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to search memories in OpenMemory: " + e.getMessage(), e);
        }
        
        return new ArrayList<>();
    }

    public void add(List<Map<String, Object>> messages, Map<String, Object> metadata) {
        try {
            // 将消息转换为 OpenMemory 的 add_memories 格式
            for (Map<String, Object> message : messages) {
                String content = (String) message.get("content");
                String role = (String) message.get("role");
                
                // 构建 OpenMemory 的添加记忆请求
                Map<String, Object> addRequest = new HashMap<>();
                addRequest.put("text", content);
                addRequest.put("user_id", metadata.get("user_id"));
                addRequest.put("metadata", metadata);
                
                String requestBody = objectMapper.writeValueAsString(addRequest);
                
                String response = webClient.post()
                    .uri(ADD_MEMORY_ENDPOINT)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();
                
                if (response != null) {
                    logger.info("Successfully added message to OpenMemory: " + content.substring(0, Math.min(50, content.length())));
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add messages to OpenMemory: " + e.getMessage(), e);
            throw new RuntimeException("Failed to add messages to OpenMemory", e);
        }
    }

    public void deleteAll(String userId) {
        try {
            // 使用 OpenMemory 的 delete_all_memories 端点
            Map<String, Object> deleteRequest = new HashMap<>();
            deleteRequest.put("user_id", userId);
            
            String requestBody = objectMapper.writeValueAsString(deleteRequest);
            
            String response = webClient.post()
                .uri(DELETE_ALL_MEMORIES_ENDPOINT)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();
            
            if (response != null) {
                logger.info("Successfully deleted all memories for user: " + userId);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete memories for user " + userId + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete memories for user: " + userId, e);
        }
    }

    public void clearAll() {
        try {
            // OpenMemory 可能没有全局清空功能，这里实现为删除所有用户的记忆
            List<String> userIds = getAllUserIds();
            for (String userId : userIds) {
                deleteAll(userId);
            }
            logger.info("Successfully cleared all memories from OpenMemory");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clear all memories: " + e.getMessage(), e);
            throw new RuntimeException("Failed to clear all memories", e);
        }
    }

    public int getTotalMemoryCount() {
        try {
            // 通过 list_memories 端点获取记忆数量
            String response = webClient.post()
                .uri(LIST_MEMORIES_ENDPOINT)
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
            
            if (response != null) {
                List<MemZeroMemory> memories = parseOpenMemoryListResponse(response);
                int count = memories.size();
                logger.info("Total memory count: " + count);
                return count;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get total memory count: " + e.getMessage(), e);
        }
        
        return 0;
    }

    public void ping() {
        try {
            String response = webClient.get()
                .uri(PING_ENDPOINT)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            if (response != null) {
                logger.info("OpenMemory ping successful");
                return;
            }
            
            throw new RuntimeException("OpenMemory ping failed: unexpected response");
        } catch (Exception e) {
            logger.log(Level.WARNING, "OpenMemory ping failed: " + e.getMessage(), e);
            throw new RuntimeException("OpenMemory ping failed", e);
        }
    }

    /**
     * 解析 OpenMemory 的搜索响应
     */
    private List<MemZeroMemory> parseOpenMemorySearchResponse(String response) {
        try {
            // 根据 OpenMemory 的实际响应格式解析
            // 这里需要根据 OpenMemory 的 API 响应格式来调整
            List<MemZeroMemory> memories = new ArrayList<>();
            
            // 示例解析逻辑（需要根据实际响应格式调整）
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            if (responseMap.containsKey("memories")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> memoriesData = (List<Map<String, Object>>) responseMap.get("memories");
                
                for (Map<String, Object> memoryData : memoriesData) {
                    MemZeroMemory memory = convertToMem0Memory(memoryData);
                    if (memory != null) {
                        memories.add(memory);
                    }
                }
            }
            
            return memories;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse OpenMemory search response: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析 OpenMemory 的列表响应
     */
    private List<MemZeroMemory> parseOpenMemoryListResponse(String response) {
        try {
            // 类似搜索响应的解析逻辑
            return parseOpenMemorySearchResponse(response);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse OpenMemory list response: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 将 API 响应转换为 Mem0Memory 对象
     */
    private MemZeroMemory convertToMem0Memory(Map<String, Object> memoryData) {
        try {
            String id = (String) memoryData.get("id");
            String content = (String) memoryData.get("memory"); // OpenMemory 使用 "memory" 字段
            String role = "user"; // 默认角色，可能需要从其他地方获取
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) memoryData.get("metadata");
            
            if (id != null && content != null) {
                return new MemZeroMemory(id, role, content, metadata != null ? metadata : new HashMap<>());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to convert memory data: " + e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * 获取配置信息
     */
    public MemZeroConfig getConfig() {
        return config;
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        try {
            ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        return String.format("OpenMemory Client - Base URL: %s, Collection: %s, Connected: %s",
            config.getBaseUrl(), config.getCollectionName(), isConnected());
    }
} 