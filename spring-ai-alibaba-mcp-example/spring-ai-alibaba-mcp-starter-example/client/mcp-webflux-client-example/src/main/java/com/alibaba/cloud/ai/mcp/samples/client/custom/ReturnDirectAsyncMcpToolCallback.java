package com.alibaba.cloud.ai.mcp.samples.client.custom;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReturnDirectAsyncMcpToolCallback extends AsyncMcpToolCallback {

    private final McpSchema.Tool tool;

    private final List<String> returnDirectToolNameList;

    private final McpAsyncClient mcpClient;



    /**
     * Creates a new {@code SyncMcpToolCallback} instance.
     *
     * @param mcpClient the MCP client to use for tool execution
     * @param tool      the MCP tool definition to adapt
     */
    public ReturnDirectAsyncMcpToolCallback(McpAsyncClient mcpClient, McpSchema.Tool tool) {
        super(mcpClient, tool);
        this.tool = tool;
        this.mcpClient = mcpClient;
        returnDirectToolNameList = new ArrayList<>();
        returnDirectToolNameList.add("generateEntrySchoolChargeDetails");
        returnDirectToolNameList.add("queryChargeDetailByIdentityCard");
        returnDirectToolNameList.add("generateReceiptIssued");
    }

    @Override
    public ToolMetadata getToolMetadata() {
        if (returnDirectToolNameList.contains(tool.name())){
            return ToolMetadata.builder()
                    .returnDirect(true)
                    .build();
        }
        return super.getToolMetadata();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return super.getToolDefinition();
    }

    @Override
    public String call(String functionInput) {
        Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
        // Note that we use the original tool name here, not the adapted one from
        // getToolDefinition
        if (!getToolMetadata().returnDirect()){
            super.call(functionInput);
        }
        return Objects.requireNonNull(this.mcpClient.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments)).map(response -> {
            if (response.isError() != null && response.isError()) {
                throw new IllegalStateException("Error calling tool: " + response.content());
            }
            return ModelOptionsUtils.toJsonString(response.content());
        }).block());
    }

    @Override
    public String call(String toolArguments, ToolContext toolContext) {
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
