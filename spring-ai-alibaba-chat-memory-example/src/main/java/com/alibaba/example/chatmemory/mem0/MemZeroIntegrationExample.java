package com.alibaba.example.chatmemory.mem0;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mem0 集成使用示例
 * 
 * 这个类展示了如何使用真正集成 Mem0 的 ChatMemoryRepository
 * 参考了 OpenMemory 的设计模式
 */
@Service
public class MemZeroIntegrationExample implements CommandLineRunner {

    @Autowired
    private MemZeroChatMemoryRepository mem0Repository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Mem0 集成示例开始 ===");
        
        // 检查 Mem0 连接状态
        checkMem0Status();
        
        // 基本操作示例
        basicOperationsExample();
        
        // 语义搜索示例
        semanticSearchExample();
        
        // 批量操作示例
        batchOperationsExample();
        
        // 错误处理示例
        errorHandlingExample();
        
        System.out.println("=== Mem0 集成示例完成 ===");
    }

    /**
     * 检查 Mem0 连接状态
     */
    private void checkMem0Status() {
        System.out.println("\n--- 检查 Mem0 状态 ---");
        
        boolean isAvailable = mem0Repository.isMem0Available();
        String status = mem0Repository.getMem0Status();
        
        System.out.println("Mem0 可用: " + isAvailable);
        System.out.println("Mem0 状态: " + status);
        
        if (!isAvailable) {
            System.out.println("警告: Mem0 不可用，将使用本地缓存模式");
        }
    }

    /**
     * 基本操作示例
     */
    private void basicOperationsExample() {
        System.out.println("\n--- 基本操作示例 ---");
        
        String userId = "user_mem0_demo";
        
        // 1. 保存用户消息
        UserMessage userMsg = new UserMessage("你好，我想了解人工智能的发展历史");
        mem0Repository.saveMessage(userId, userMsg);
        System.out.println("✓ 保存用户消息");
        
        // 2. 保存助手回复
        AssistantMessage assistantMsg = new AssistantMessage(
            "人工智能的发展可以追溯到1950年代。图灵测试的提出标志着AI研究的开始。" +
            "经历了符号主义、连接主义等不同阶段，现在进入了深度学习时代。"
        );
        mem0Repository.saveMessage(userId, assistantMsg);
        System.out.println("✓ 保存助手回复");
        
        // 3. 保存系统消息
        SystemMessage systemMsg = new SystemMessage("这是一个关于AI历史的对话");
        mem0Repository.saveMessage(userId, systemMsg);
        System.out.println("✓ 保存系统消息");
        
        // 4. 获取所有消息
        List<Message> allMessages = mem0Repository.findByConversationId(userId);
        System.out.println("✓ 获取到 " + allMessages.size() + " 条消息");
        
        // 5. 获取最后2条消息
        List<Message> recentMessages = mem0Repository.getLastMessages(userId, 2);
        System.out.println("✓ 获取到最近 " + recentMessages.size() + " 条消息");
        
        // 6. 按类型过滤消息
        List<Message> userMessages = mem0Repository.getMessagesByType(userId, "user");
        List<Message> assistantMessages = mem0Repository.getMessagesByType(userId, "assistant");
        System.out.println("✓ 用户消息: " + userMessages.size() + " 条");
        System.out.println("✓ 助手消息: " + assistantMessages.size() + " 条");
    }

    /**
     * 语义搜索示例
     */
    private void semanticSearchExample() {
        System.out.println("\n--- 语义搜索示例 ---");
        
        String userId = "user_semantic_search";
        
        // 添加一些测试数据
        mem0Repository.saveMessage(userId, new UserMessage("什么是机器学习？"));
        mem0Repository.saveMessage(userId, new AssistantMessage("机器学习是人工智能的一个分支，它使计算机能够在没有明确编程的情况下学习和改进。"));
        mem0Repository.saveMessage(userId, new UserMessage("深度学习是什么？"));
        mem0Repository.saveMessage(userId, new AssistantMessage("深度学习是机器学习的一个子集，使用多层神经网络来模拟人脑的学习过程。"));
        mem0Repository.saveMessage(userId, new UserMessage("自然语言处理有哪些应用？"));
        mem0Repository.saveMessage(userId, new AssistantMessage("自然语言处理的应用包括机器翻译、情感分析、聊天机器人、文本摘要等。"));
        
        // 进行语义搜索
        List<Message> searchResults = mem0Repository.searchMessages(userId, "神经网络", 5);
        System.out.println("✓ 搜索 '神经网络' 找到 " + searchResults.size() + " 条相关消息");
        
        searchResults = mem0Repository.searchMessages(userId, "AI应用", 5);
        System.out.println("✓ 搜索 'AI应用' 找到 " + searchResults.size() + " 条相关消息");
        
        searchResults = mem0Repository.searchMessages(userId, "计算机学习", 5);
        System.out.println("✓ 搜索 '计算机学习' 找到 " + searchResults.size() + " 条相关消息");
    }

    /**
     * 批量操作示例
     */
    private void batchOperationsExample() {
        System.out.println("\n--- 批量操作示例 ---");
        
        String userId = "user_batch_operations";
        
        // 批量保存消息
        List<Message> messages = List.of(
            new UserMessage("请介绍一下Python编程语言"),
            new AssistantMessage("Python是一种高级编程语言，以其简洁的语法和强大的库生态系统而闻名。"),
            new UserMessage("Python适合做什么？"),
            new AssistantMessage("Python适合数据科学、机器学习、Web开发、自动化脚本等多种应用场景。"),
            new UserMessage("如何学习Python？"),
            new AssistantMessage("建议从基础语法开始，然后学习面向对象编程，最后深入特定领域的应用。")
        );
        
        mem0Repository.saveAll(userId, messages);
        System.out.println("✓ 批量保存了 " + messages.size() + " 条消息");
        
        // 获取统计信息
        int conversationCount = mem0Repository.getConversationCount();
        int totalMessageCount = mem0Repository.getTotalMessageCount();
        boolean hasConversation = mem0Repository.hasConversation(userId);
        
        System.out.println("✓ 总对话数: " + conversationCount);
        System.out.println("✓ 总消息数: " + totalMessageCount);
        System.out.println("✓ 用户是否有对话: " + hasConversation);
    }

    /**
     * 错误处理示例
     */
    private void errorHandlingExample() {
        System.out.println("\n--- 错误处理示例 ---");
        
        // 测试空值处理
        try {
            mem0Repository.saveMessage(null, new UserMessage("测试"));
            System.out.println("✗ 应该抛出异常");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 正确处理空用户ID: " + e.getMessage());
        }
        
        try {
            mem0Repository.saveMessage("test_user", null);
            System.out.println("✗ 应该抛出异常");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ 正确处理空消息: " + e.getMessage());
        }
        
        // 测试空字符串处理
        List<Message> emptyResult = mem0Repository.findByConversationId("");
        System.out.println("✓ 正确处理空字符串用户ID，返回 " + emptyResult.size() + " 条消息");
        
        // 测试不存在的用户
        List<Message> nonExistentUser = mem0Repository.findByConversationId("non_existent_user");
        System.out.println("✓ 正确处理不存在的用户，返回 " + nonExistentUser.size() + " 条消息");
    }

    /**
     * 多用户场景示例
     */
    public void multiUserExample() {
        System.out.println("\n--- 多用户场景示例 ---");
        
        String[] users = {"alice", "bob", "charlie"};
        
        // 为每个用户添加不同的对话
        for (String user : users) {
            mem0Repository.saveMessage(user, new UserMessage(user + " 说: 你好"));
            mem0Repository.saveMessage(user, new AssistantMessage("你好 " + user + "！很高兴见到你。"));
        }
        
        // 获取所有用户ID
        List<String> allUserIds = mem0Repository.findConversationIds();
        System.out.println("✓ 所有用户: " + allUserIds);
        
        // 为每个用户获取消息
        for (String userId : allUserIds) {
            List<Message> messages = mem0Repository.findByConversationId(userId);
            System.out.println("✓ " + userId + " 的消息数量: " + messages.size());
        }
    }

    /**
     * 清理测试数据
     */
    public void cleanupTestData() {
        System.out.println("\n--- 清理测试数据 ---");
        
        String[] testUsers = {
            "user_mem0_demo",
            "user_semantic_search", 
            "user_batch_operations",
            "alice",
            "bob",
            "charlie"
        };
        
        for (String userId : testUsers) {
            mem0Repository.deleteByConversationId(userId);
            System.out.println("✓ 删除用户 " + userId + " 的对话");
        }
        
        System.out.println("✓ 测试数据清理完成");
    }
} 