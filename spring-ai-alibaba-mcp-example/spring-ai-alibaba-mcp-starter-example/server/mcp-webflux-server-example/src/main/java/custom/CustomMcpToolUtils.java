//package custom;
//
//import io.modelcontextprotocol.server.McpServerFeatures;
//import io.modelcontextprotocol.server.McpSyncServerExchange;
//import io.modelcontextprotocol.spec.McpSchema;
//import org.springframework.ai.chat.model.ToolContext;
//import org.springframework.ai.mcp.McpToolUtils;
//import org.springframework.ai.model.ModelOptionsUtils;
//import org.springframework.ai.tool.ToolCallback;
//import org.springframework.util.MimeType;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.util.List;
//import java.util.Map;
//
//public class CustomMcpToolUtils {
//
//    public static CustomMcpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(ToolCallback toolCallback,
//                                                                                    MimeType mimeType) {
//
//        CustomMcpServerFeatures.SyncToolSpecification syncToolSpecification = CustomMcpToolUtils.toSyncToolSpecification(toolCallback, mimeType);
//
//        return new CustomMcpServerFeatures.AsyncToolSpecification(syncToolSpecification.tool(),
//                (exchange, map) -> Mono
//                        .fromCallable(() -> syncToolSpecification.call().apply(new McpSyncServerExchange(exchange), map))
//                        .subscribeOn(Schedulers.boundedElastic()));
//    }
//
//    public static CustomMcpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback,
//                                                                                  MimeType mimeType) {
//
//        var tool = new CustomMcpSchema.Tool(toolCallback.getToolDefinition().name(),
//                toolCallback.getToolDefinition().description(), toolCallback.getToolMetadata().returnDirect(), toolCallback.getToolDefinition().inputSchema());
//
//        return new CustomMcpServerFeatures.SyncToolSpecification(tool, (exchange, request) -> {
//            try {
//                String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request),
//                        new ToolContext(Map.of(McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)));
//                if (mimeType != null && mimeType.toString().startsWith("image")) {
//                    return new McpSchema.CallToolResult(List
//                            .of(new McpSchema.ImageContent(List.of(McpSchema.Role.ASSISTANT), null, callResult, mimeType.toString())),
//                            false);
//                }
//                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
//            }
//            catch (Exception e) {
//                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true);
//            }
//        });
//    }
//}
