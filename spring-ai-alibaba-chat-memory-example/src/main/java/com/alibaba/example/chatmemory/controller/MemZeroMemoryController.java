package com.alibaba.example.chatmemory.controller;

import com.alibaba.example.chatmemory.mem0.MemZeroChatMemoryAdvisor;
import com.alibaba.example.chatmemory.mem0.MemZeroServerRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author miaoyumeng
 * @date 2025/06/23 11:54
 * @description TODO
 */
@RestController
@RequestMapping("/advisor/memory/mem0")
public class MemZeroMemoryController {
    private final ChatClient chatClient;
    private final VectorStore store;

    public MemZeroMemoryController(ChatClient.Builder builder, VectorStore store) {
        this.store = store;
        this.chatClient = builder
                .defaultAdvisors(
                        MemZeroChatMemoryAdvisor.builder(store).build()
                )
                .build();
    }

    @GetMapping("/call")
    public String call(@RequestParam(value = "query", defaultValue = "你好，我是万能的喵，我爱玩三角洲行动") String query,
                       @RequestParam(value = "conversation_id", defaultValue = "user") String conversationId
    ) {
        return chatClient.prompt(query)
                .advisors(
                        a -> a.param(CONVERSATION_ID, conversationId)
                )
                .call().content();
    }

    @GetMapping("/messages")
    public List<Document> messages(@RequestParam(value = "conversation_id", defaultValue = "user") String conversationId) {
        MemZeroServerRequest.SearchRequest searchRequest = MemZeroServerRequest.SearchRequest.builder().query("我的爱好是什么？").userId("miao").build();
        List<Document> documents = store.similaritySearch(searchRequest);
        return documents;
    }
}
