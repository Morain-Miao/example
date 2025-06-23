package com.alibaba.example.chatmemory.mem0;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemZeroMemory {

    // 关系数据
    private List<MemZeroResults> results;

    // 关系数据
    private List<MemZeroRelation> relations;

    public MemZeroMemory() {
        this.relations = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    public MemZeroMemory(List<MemZeroResults> results, List<MemZeroRelation> relations) {
        this.results = results;
        this.relations = relations;
    }

    public List<MemZeroResults> getResults() {
        return results;
    }

    public void setResults(List<MemZeroResults> results) {
        this.results = results;
    }

    public List<MemZeroRelation> getRelations() {
        return relations;
    }

    public void setRelations(List<MemZeroRelation> relations) {
        this.relations = relations;
    }

    /**
     * Mem0 关系数据模型
     * 对应 Mem0 服务返回的 relations 数组中的每个关系对象
     */
    public static class MemZeroRelation {
        private String source;        // 源节点
        private String relationship;  // 关系类型
        private String target;        // 目标节点

        // 默认构造函数
        public MemZeroRelation() {}

        // 完整构造函数
        public MemZeroRelation(String source, String relationship, String target) {
            this.source = source;
            this.relationship = relationship;
            this.target = target;
        }

        // Getters and Setters
        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getRelationship() {
            return relationship;
        }

        public void setRelationship(String relationship) {
            this.relationship = relationship;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        @Override
        public String toString() {
            return "MemZeroRelation{" +
                    "source='" + source + '\'' +
                    ", relationship='" + relationship + '\'' +
                    ", target='" + target + '\'' +
                    '}';
        }
    }

    public static class MemZeroResults {
        private String id;
        private String memory; // 实际的记忆内容
        private String hash;
        private Map<String, Object> metadata;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("created_at")
        private ZonedDateTime createdAt;
        
        @JsonProperty("updated_at")
        private ZonedDateTime updatedAt;
        
        @JsonProperty("agent_id")
        private String agentId;
        
        @JsonProperty("run_id")
        private String runId;

        public MemZeroResults() {
        }

        public MemZeroResults(String id, String memory, String hash, Map<String, Object> metadata, String userId, ZonedDateTime createdAt, ZonedDateTime updatedAt, String agentId, String runId) {
            this.id = id;
            this.memory = memory;
            this.hash = hash;
            this.metadata = metadata;
            this.userId = userId;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.agentId = agentId;
            this.runId = runId;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMemory() {
            return memory;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
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

        public ZonedDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(ZonedDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public ZonedDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(ZonedDateTime updatedAt) {
            this.updatedAt = updatedAt;
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

        @Override
        public String toString() {
            return "MemZeroResults{" +
                    "id='" + id + '\'' +
                    ", memory='" + memory + '\'' +
                    ", hash='" + hash + '\'' +
                    ", metadata=" + metadata +
                    ", userId='" + userId + '\'' +
                    ", createdAt=" + createdAt +
                    ", updatedAt=" + updatedAt +
                    ", agentId='" + agentId + '\'' +
                    ", runId='" + runId + '\'' +
                    '}';
        }
    }

}
