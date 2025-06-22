package com.alibaba.example.chatmemory.mem0;

import java.util.Map;

public class MemZeroMemory {
    private final String id;
    private final String role;
    private final String content;
    private final Map<String, Object> metadata;

    public MemZeroMemory(String id, String role, String content, Map<String, Object> metadata) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.metadata = metadata;
    }

    public String getId() { return id; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Map<String, Object> getMetadata() { return metadata; }
}
