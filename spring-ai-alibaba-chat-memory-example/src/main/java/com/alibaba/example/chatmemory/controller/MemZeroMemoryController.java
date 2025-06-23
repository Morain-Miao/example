package com.alibaba.example.chatmemory.controller;

import com.alibaba.example.chatmemory.mem0.MemZeroChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/advisor/memory/mem0")
public class MemZeroMemoryController {
    @Autowired
    private MemZeroChatMemoryRepository mem0ChatMemoryRepository;

    private final ChatClient chatClient;
    private final int MAX_MESSAGES = 100;
    private final MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(mem0ChatMemoryRepository)
            .maxMessages(MAX_MESSAGES)
            .build();

    public MemZeroMemoryController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                                .build()
                )
                .build();
    }

    @GetMapping("/call")
    public String call(@RequestParam(value = "query", defaultValue = "你好，我是万能的喵。我爱玩魔兽世界") String query,
                       @RequestParam(value = "conversation_id", defaultValue = "miao") String conversationId
    ) {
        return chatClient.prompt(query)
                .advisors(
                        a -> a.param(CONVERSATION_ID, conversationId)
                )
                .call().content();
    }

    @GetMapping("/messages")
    public List<Message> messages(@RequestParam(value = "conversation_id", defaultValue = "miao") String conversationId) {
        return messageWindowChatMemory.get(conversationId);
    }
}
