package com.alibaba.example.chatmemory.mem0;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import com.alibaba.cloud.ai.memory.redis.serializer.MessageDeserializer;
import com.alibaba.example.chatmemory.config.MemZeroChatMemoryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * MemZeroMemoryRepository
 */
public class MemZeroChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger logger = Logger.getLogger(MemZeroChatMemoryRepository.class.getName());

    private final MemZeroHttpClient mem0Client;

    private MemZeroChatMemoryRepository(MemZeroHttpClient mem0Client) {
        Assert.notNull(mem0Client, "mem0Client cannot be null");
        this.mem0Client = mem0Client;
    }

    // 内存缓存作为回退机制
    private final Map<String, List<Message>> conversationCache = new ConcurrentHashMap<>();

    public static MemZeroChatMemoryBuilder builder() {
        return new MemZeroChatMemoryBuilder();
    }

    public static class MemZeroChatMemoryBuilder {

        private MemZeroChatMemoryProperties properties;

        private MemZeroHttpClient mem0Client;



        public MemZeroChatMemoryRepository.MemZeroChatMemoryBuilder properties(MemZeroChatMemoryProperties properties) {
            this.properties = properties;
            return this;
        }

        public MemZeroChatMemoryRepository.MemZeroChatMemoryBuilder memZeroChatMemoryClient(MemZeroHttpClient mem0Client) {
            this.mem0Client = mem0Client;
            return this;
        }

        public MemZeroChatMemoryRepository build() {
            if (mem0Client == null){
                MemZeroHttpClient client = new MemZeroHttpClient(properties);
                return new MemZeroChatMemoryRepository(client);
            }
            return new MemZeroChatMemoryRepository(mem0Client);
        }

    }


    /**
     * 查找所有对话ID（用户ID）
     * 
     * @return 所有存储了对话的用户ID列表
     */
    @Override
    public List<String> findConversationIds() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * 根据用户ID查找所有消息
     * 
     * @param userId 用户ID
     * @return 该用户的所有消息列表
     */
    @Override
    public List<Message> findByConversationId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            if (mem0Client != null) {
                // 从 Mem0 获取该用户的所有记忆
                MemZeroMemory memories = mem0Client.getAllMemories(userId, null, null);
                return convertMem0MemoriesToMessages(memories);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get messages from Mem0 for user " + userId + ": " + e.getMessage(), e);
        }
        
        // 回退到缓存
        return conversationCache.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 保存所有消息到指定用户的对话
     * 
     * @param userId 用户ID
     * @param messages 要保存的消息列表
     */
    @Override
    public void saveAll(String userId, List<Message> messages) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        try {
            if (mem0Client != null) {
                // 转换消息为 Mem0 格式
                List<MemZeroRequest.Message> mem0Messages = messages.stream()
                    .map(this::convertMessageToMem0Format)
                    .collect(Collectors.toList());
                
                // 创建 MemoryCreate 请求
                MemZeroRequest.MemoryCreate memoryCreate = new MemZeroRequest.MemoryCreate();
                memoryCreate.setMessages(mem0Messages);
                memoryCreate.setUserId(userId);

                // 保存到 Mem0
                mem0Client.addMemory(memoryCreate);
                logger.info("Saved " + messages.size() + " messages to Mem0 for user " + userId);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to save messages to Mem0 for user " + userId + ": " + e.getMessage(), e);
        }
        
        // 同时更新缓存
        List<Message> existingMessages = conversationCache.getOrDefault(userId, new ArrayList<>());
        List<Message> allMessages = new ArrayList<>(existingMessages);
        allMessages.addAll(messages);
        conversationCache.put(userId, allMessages);
    }

    /**
     * 删除指定用户的所有对话
     * 
     * @param userId 用户ID
     */
    @Override
    public void deleteByConversationId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        
        try {
            if (mem0Client != null) {
                // 从 Mem0 删除该用户的所有记忆
                mem0Client.deleteAllMemories(userId, null, null);
                logger.info("Deleted all memories from Mem0 for user " + userId);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete memories from Mem0 for user " + userId + ": " + e.getMessage(), e);
        }
        
        // 同时清除缓存
        conversationCache.remove(userId);
    }

    /**
     * 保存单条消息
     * 
     * @param userId 用户ID
     * @param message 要保存的消息
     */
    public void saveMessage(String userId, Message message) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        saveAll(userId, List.of(message));
    }

    /**
     * 获取用户最后N条消息
     * 
     * @param userId 用户ID
     * @param limit 最大返回消息数
     * @return 最后N条消息
     */
    public List<Message> getLastMessages(String userId, int limit) {
        List<Message> messages = findByConversationId(userId);
        if (messages.size() <= limit) {
            return new ArrayList<>(messages);
        }
        return messages.subList(messages.size() - limit, messages.size());
    }

    /**
     * 清空所有对话
     */
    public void clearAll() {
        try {
            if (mem0Client != null) {
                // 从 Mem0 清空所有记忆
                mem0Client.resetAllMemories();
                logger.info("Cleared all memories from Mem0");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clear all memories from Mem0: " + e.getMessage(), e);
        }
        
        // 清空缓存
        conversationCache.clear();
    }


    /**
     * 检查用户是否有对话
     * 
     * @param userId 用户ID
     * @return 是否有对话
     */
    public boolean hasConversation(String userId) {
        List<Message> messages = findByConversationId(userId);
        return !messages.isEmpty();
    }

    /**
     * 按消息类型过滤消息
     * 
     * @param userId 用户ID
     * @param messageType 消息类型
     * @return 过滤后的消息列表
     */
    public List<Message> getMessagesByType(String userId, String messageType) {
        List<Message> messages = findByConversationId(userId);
        return messages.stream()
                .filter(message -> message.getMessageType().getValue().equalsIgnoreCase(messageType))
                .collect(Collectors.toList());
    }

    /**
     * 语义搜索消息
     * 
     * @param userId 用户ID
     * @param query 搜索查询
     * @param limit 最大返回数量
     * @return 搜索结果
     */
    public List<Message> searchMessages(String userId, String query, int limit) {
        try {
            if (mem0Client != null) {
                // 使用 Mem0 进行语义搜索
                MemZeroRequest.SearchRequest searchRequest = new MemZeroRequest.SearchRequest();
                searchRequest.setQuery(query);
                searchRequest.setUserId(userId);
                
                MemZeroMemory memories = mem0Client.searchMemories(searchRequest);
                List<Message> results = convertMem0MemoriesToMessages(memories);
                
                // 限制结果数量
                if (results.size() > limit) {
                    results = results.subList(0, limit);
                }
                
                return results;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to search messages in Mem0: " + e.getMessage(), e);
        }
        
        // 回退到简单的文本搜索
        List<Message> messages = findByConversationId(userId);
        return messages.stream()
                .filter(message -> message.getText().toLowerCase().contains(query.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 将 Spring AI 消息转换为 Mem0 格式
     */
    private MemZeroRequest.Message convertMessageToMem0Format(Message message) {
        return new MemZeroRequest.Message(message.getMessageType().getValue(), message.getText());
    }

    /**
     * 将 Mem0 记忆转换为 Spring AI 消息
     */
    private List<Message> convertMem0MemoriesToMessages(MemZeroMemory memories) {

        List<Message> messages = new ArrayList<>();

        List<MemZeroMemory.MemZeroResults> results = memories.getResults();
        List<MemZeroMemory.MemZeroRelation> relations = memories.getRelations();

        results.forEach(result -> {
            messages.add(SystemMessage.builder()
                    .text(result.toString())
                    .build());
        });

        relations.forEach(relation -> {
            messages.add(SystemMessage.builder()
                    .text(relation.toString())
                    .build());
        });


        return messages;
    }
}