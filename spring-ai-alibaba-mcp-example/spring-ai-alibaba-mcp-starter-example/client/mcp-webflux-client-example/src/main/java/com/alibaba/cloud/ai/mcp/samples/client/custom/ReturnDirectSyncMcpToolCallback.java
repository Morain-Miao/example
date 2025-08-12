package com.alibaba.cloud.ai.mcp.samples.client.custom;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Map;

public class ReturnDirectSyncMcpToolCallback  extends SyncMcpToolCallback {

    private final Tool tool;

    private final McpSyncClient mcpClient;



    /**
     * Creates a new {@code SyncMcpToolCallback} instance.
     *
     * @param mcpClient the MCP client to use for tool execution
     * @param tool      the MCP tool definition to adapt
     */
    public ReturnDirectSyncMcpToolCallback(McpSyncClient mcpClient, Tool tool) {
        super(mcpClient, tool);
        this.tool = tool;
        this.mcpClient = mcpClient;
    }

    @NotNull
    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder()
                .returnDirect(tool.annotations().returnDirect())
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return super.getToolDefinition();
    }

    @NotNull
    @Override
    public String call(@NotNull String functionInput) {
        Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
        // Note that we use the original tool name here, not the adapted one from
        // getToolDefinition
        if (!getToolMetadata().returnDirect()){
            super.call(functionInput);
        }

        McpSchema.CallToolResult response = this.mcpClient.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments));
        if (response.isError() != null && response.isError()) {
            throw new IllegalStateException("Error calling tool: " + response.content());
        }
        return ModelOptionsUtils.toJsonString(response.content());
    }

    @NotNull
    @Override
    public String call(@NotNull String toolArguments, @NotNull ToolContext toolContext) {
        return super.call(toolArguments, toolContext);
    }


    /**
     * 转换转义的HTML字符串为正常HTML
     */
    private String convertEscapedHtml(String escapedHtml) {
        if (escapedHtml == null || escapedHtml.isEmpty()) {
            return escapedHtml;
        }

        // 替换转义字符
        return escapedHtml
                .replace("\\n", "\n")           // 换行符
                .replace("\\r", "\r")           // 回车符
                .replace("\\t", "\t")           // 制表符
                .replace("\\\"", "\"")          // 双引号
                .replace("\\'", "'")            // 单引号
                .replace("\\\\", "\\");         // 反斜杠
    }

    private String convertJsonHtml(String jsonHtml) {
        if (jsonHtml == null || jsonHtml.isEmpty()) {
            return jsonHtml;
        }

        // 移除JSON字符串的开头和结尾引号
        String html = jsonHtml;
        if (html.startsWith("\"") && html.endsWith("\"")) {
            html = html.substring(1, html.length() - 1);
        }

        return convertEscapedHtml(html);
    }
}
