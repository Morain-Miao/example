package com.alibaba.example.chatmemory.mem0;


public class MemZeroConfig {
    private final String apiKey;
    private final String baseUrl;
    private final String collectionName;

    private MemZeroConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.collectionName = builder.collectionName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getCollectionName() { return collectionName; }

    public static class Builder {
        private String apiKey;
        private String baseUrl = "https://api.mem0.ai";
        private String collectionName = "spring-ai-chat";

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder collectionName(String collectionName) {
            this.collectionName = collectionName;
            return this;
        }

        public MemZeroConfig build() {
            return new MemZeroConfig(this);
        }
    }
}
