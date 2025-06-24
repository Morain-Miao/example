package com.alibaba.example.chatmemory.config;

import com.alibaba.cloud.ai.autoconfigure.memory.ChatMemoryAutoConfiguration;
import com.alibaba.example.chatmemory.mem0.MemZeroChatMemoryRepository;
import com.alibaba.example.chatmemory.mem0.MemZeroMemoryStore;
import com.alibaba.example.chatmemory.mem0.MemZeroServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        before = {ChatMemoryAutoConfiguration.class}
)
@ConditionalOnClass({MemZeroChatMemoryRepository.class, MemZeroChatMemoryProperties.class})
public class MemZeroChatMemoryAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MemZeroChatMemoryAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public MemZeroChatMemoryProperties memZeroChatMemoryProperties() {
        return new MemZeroChatMemoryProperties();
    }

    @Bean
    @ConditionalOnBean(MemZeroChatMemoryProperties.class)
    public MemZeroServiceClient elasticsearchRestClient(MemZeroChatMemoryProperties properties) {
        logger.info("Initializing MemZeroService RestClient.");
        return new MemZeroServiceClient(properties);
    }

    @Bean
    @ConditionalOnBean(MemZeroServiceClient.class)
    public VectorStore memZeroMemoryStore(MemZeroServiceClient client) {
        //TODO 客户端初始化后，需要初始化一系列python中的配置
        return MemZeroMemoryStore.builder(client).build();
    }
}
