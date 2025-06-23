package com.alibaba.example.chatmemory.config;

import com.alibaba.cloud.ai.autoconfigure.memory.ChatMemoryAutoConfiguration;
import com.alibaba.example.chatmemory.mem0.MemZeroChatMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        before = {ChatMemoryAutoConfiguration.class}
)
@ConditionalOnClass({MemZeroChatMemoryRepository.class, MemZeroChatMemoryProperties.class})
public class MemZeroChatMemoryAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MemZeroChatMemoryAutoConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "mem0.api")
    @ConditionalOnMissingBean
    public MemZeroChatMemoryProperties memZeroChatMemoryProperties() {
        return new MemZeroChatMemoryProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    MemZeroChatMemoryRepository memZeroChatMemoryRepository(MemZeroChatMemoryProperties properties) {
        logger.info("Configuring Mem0 chat memory repository");
        return MemZeroChatMemoryRepository.builder().properties(properties).build();
    }
}
