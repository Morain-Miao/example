package com.alibaba.cloud.ai.mcp.samples.client.custom;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 聊天客户端配置
 *
 * @author dawei
 */
@Configuration
public class ChatClientConfig {

    /**
     * 聊天客户端
     */
    @Bean
    @ConditionalOnClass(SyncMcpToolCallbackProvider.class)
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider, List<McpSyncClient> syncClients) {

        // 日志记录
        SimpleLoggerAdvisor customLoggerAdvisor = new SimpleLoggerAdvisor(
                request -> "[chat request]: " + request.prompt(),
                response -> "[chat response]: " + response.getResult(),
                0
        );

        List<ReturnDirectSyncMcpToolCallback> dynamicSyncMcpToolCallbacks = syncClients.stream().flatMap(mcpClient -> {
            SyncMcpToolCallbackProvider asyncMcpToolCallbackProvider = new SyncMcpToolCallbackProvider(mcpClient);
            ToolCallback[] toolCallbacks = asyncMcpToolCallbackProvider.getToolCallbacks();
            return Arrays.stream(toolCallbacks).map(toolCallback -> {
                McpServerFeatures.SyncToolSpecification syncToolSpecification = McpToolUtils.toSyncToolSpecification(toolCallback);
                return new ReturnDirectSyncMcpToolCallback(mcpClient,
                        syncToolSpecification.tool());
            });
        }).toList();


        return builder
                .defaultAdvisors(customLoggerAdvisor)
                .defaultToolCallbacks(ToolCallbackProvider.from(dynamicSyncMcpToolCallbacks))
                .build();
    }

}
