package com.alibaba.example.chatmemory.mem0;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class MemZeroRequest {
    
    public static class Message {
        private String role;
        private String content;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    public static class MemoryCreate {
        private List<Message> messages;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("agent_id")
        private String agentId;
        
        @JsonProperty("run_id")
        private String runId;
        
        private Map<String, Object> metadata;
        
        public MemoryCreate() {}
        
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }
        
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class SearchRequest {
        private String query;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("run_id")
        private String runId;
        
        @JsonProperty("agent_id")
        private String agentId;
        
        private Map<String, Object> filters;
        
        public SearchRequest() {}
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        
        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }
        
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    }
} 