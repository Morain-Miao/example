package com.alibaba.example.chatmemory.mem0;

import com.alibaba.example.chatmemory.config.MemZeroConfig;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 真正集成 Mem0 的 ChatMemoryRepository 实现
 * 
 * 这个实现参考了 OpenMemory 的设计，真正调用 mem0 的内存系统
 * 来存储和检索对话历史，而不是仅仅使用内存存储。
 * 
 * 特性:
 * - 真正的 mem0 集成，使用向量存储和 LLM
 * - 支持语义搜索和记忆检索
 * - 线程安全的操作
 * - 完整的错误处理和回退机制
 * - 支持多种消息类型
 */
@Component
public class MemZeroChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger logger = Logger.getLogger(MemZeroChatMemoryRepository.class.getName());
    
    // Mem0 客户端
    @Autowired
    private MemZeroHttpClient mem0Client;

    @Autowired
    private MemZeroConfig memZeroConfig;

    // 内存缓存作为回退机制
    private final Map<String, List<Message>> conversationCache = new ConcurrentHashMap<>();

    /**
     * 查找所有对话ID（用户ID）
     * 
     * @return 所有存储了对话的用户ID列表
     */
    @Override
    public List<String> findConversationIds() {
        try {
            if (mem0Client != null) {
                // 从 Mem0 获取所有记忆，然后提取用户ID
                List<MemZeroMemory> allMemories = mem0Client.getAllMemories(null, null, null);
                return allMemories.stream()
                    .map(MemZeroMemory::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get conversation IDs from Mem0: " + e.getMessage(), e);
        }
        
        // 回退到缓存
        return new ArrayList<>(conversationCache.keySet());
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
                List<MemZeroMemory> memories = mem0Client.getAllMemories(userId, null, null);
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
     * 获取对话数量
     * 
     * @return 对话数量
     */
    public int getConversationCount() {
        return findConversationIds().size();
    }

    /**
     * 获取总消息数
     * 
     * @return 总消息数
     */
    public int getTotalMessageCount() {
        try {
            if (mem0Client != null) {
                // 获取所有记忆并计算总数
                List<MemZeroMemory> allMemories = mem0Client.getAllMemories(null, null, null);
                return allMemories.size();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get total message count from Mem0: " + e.getMessage(), e);
        }
        
        // 回退到缓存统计
        return conversationCache.values().stream()
                .mapToInt(List::size)
                .sum();
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
                .filter(message -> getMessageType(message).equalsIgnoreCase(messageType))
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
                
                List<MemZeroMemory> memories = mem0Client.searchMemories(searchRequest);
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
        return new MemZeroRequest.Message(getMessageType(message), message.getText());
    }

    /**
     * 将 Mem0 记忆转换为 Spring AI 消息
     */
    private List<Message> convertMem0MemoriesToMessages(List<MemZeroMemory> memories) {
        return memories.stream()
                .map(this::convertMem0MemoryToMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将单个 Mem0 记忆转换为 Spring AI 消息
     */
    private Message convertMem0MemoryToMessage(MemZeroMemory memory) {
        String role = memory.getRole();
        String content = memory.getContent();
        
        switch (role.toLowerCase()) {
            case "user":
                return new UserMessage(content);
            case "assistant":
                return new AssistantMessage(content);
            case "system":
                return new SystemMessage(content);
            default:
                logger.warning("Unknown message role: " + role);
                return new UserMessage(content);
        }
    }

    /**
     * 获取消息类型
     */
    private String getMessageType(Message message) {
        if (message instanceof UserMessage) {
            return "user";
        } else if (message instanceof AssistantMessage) {
            return "assistant";
        } else if (message instanceof SystemMessage) {
            return "system";
        } else {
            return "unknown";
        }
    }

    /**
     * 检查 Mem0 客户端是否可用
     */
    public boolean isMem0Available() {
        return mem0Client != null;
    }

    /**
     * 获取 Mem0 客户端状态
     */
    public String getMem0Status() {
        if (mem0Client != null) {
            try {
                boolean isHealthy = mem0Client.ping();
                return isHealthy ? "CONNECTED" : "UNHEALTHY";
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }
        return "NOT_AVAILABLE";
    }
}