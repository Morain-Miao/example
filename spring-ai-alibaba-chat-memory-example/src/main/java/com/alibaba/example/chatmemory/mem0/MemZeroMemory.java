package com.alibaba.example.chatmemory.mem0;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public class MemZeroMemory {
    private String id;
    private String role;
    private String content;
    private Map<String, Object> metadata;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("agent_id")
    private String agentId;
    
    @JsonProperty("run_id")
    private String runId;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    private Double score; // 用于搜索结果的相关性分数

    // 默认构造函数
    public MemZeroMemory() {}
    
    // 完整构造函数
    public MemZeroMemory(String id, String role, String content, Map<String, Object> metadata) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getId() { 
        return id; 
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRole() { 
        return role; 
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() { 
        return content; 
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Map<String, Object> getMetadata() { 
        return metadata; 
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
}
