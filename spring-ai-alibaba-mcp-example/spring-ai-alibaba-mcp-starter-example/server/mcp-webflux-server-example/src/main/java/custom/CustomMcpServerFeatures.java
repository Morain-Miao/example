//package custom;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.modelcontextprotocol.server.McpAsyncServerExchange;
//import io.modelcontextprotocol.server.McpServerFeatures;
//import io.modelcontextprotocol.server.McpSyncServerExchange;
//import io.modelcontextprotocol.spec.McpSchema;
//import io.modelcontextprotocol.util.Assert;
//import io.modelcontextprotocol.util.Utils;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.function.BiConsumer;
//import java.util.function.BiFunction;
//
//public class CustomMcpServerFeatures extends McpServerFeatures {
//
//    record Async(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
//                 List<McpServerFeatures.AsyncToolSpecification> tools, Map<String, AsyncResourceSpecification> resources,
//                 List<McpSchema.ResourceTemplate> resourceTemplates,
//                 Map<String, McpServerFeatures.AsyncPromptSpecification> prompts,
//                 Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions,
//                 List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
//                 String instructions) {
//
//        /**
//         * Create an instance and validate the arguments.
//         * @param serverInfo The server implementation details
//         * @param serverCapabilities The server capabilities
//         * @param tools The list of tool specifications
//         * @param resources The map of resource specifications
//         * @param resourceTemplates The list of resource templates
//         * @param prompts The map of prompt specifications
//         * @param rootsChangeConsumers The list of consumers that will be notified when
//         * the roots list changes
//         * @param instructions The server instructions text
//         */
//        Async(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
//              List<McpServerFeatures.AsyncToolSpecification> tools, Map<String, AsyncResourceSpecification> resources,
//              List<McpSchema.ResourceTemplate> resourceTemplates,
//              Map<String, McpServerFeatures.AsyncPromptSpecification> prompts,
//              Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions,
//              List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
//              String instructions) {
//
//            Assert.notNull(serverInfo, "Server info must not be null");
//
//            this.serverInfo = serverInfo;
//            this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
//                    : new McpSchema.ServerCapabilities(null, // completions
//                    null, // experimental
//                    new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
//                    // logging
//                    // by
//                    // default
//                    !Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
//                    !Utils.isEmpty(resources)
//                            ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
//                    !Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);
//
//            this.tools = (tools != null) ? tools : List.of();
//            this.resources = (resources != null) ? resources : Map.of();
//            this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : List.of();
//            this.prompts = (prompts != null) ? prompts : Map.of();
//            this.completions = (completions != null) ? completions : Map.of();
//            this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : List.of();
//            this.instructions = instructions;
//        }
//
//        /**
//         * Convert a synchronous specification into an asynchronous one and provide
//         * blocking code offloading to prevent accidental blocking of the non-blocking
//         * transport.
//         * @param syncSpec a potentially blocking, synchronous specification.
//         * @return a specification which is protected from blocking calls specified by the
//         * user.
//         */
//        static CustomMcpServerFeatures.Async fromSync(CustomMcpServerFeatures.Sync syncSpec) {
//            List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
//            for (var tool : syncSpec.tools()) {
//                tools.add(McpServerFeatures.AsyncToolSpecification.fromSync(tool));
//            }
//
//            Map<String, AsyncResourceSpecification> resources = new HashMap<>();
//            syncSpec.resources().forEach((key, resource) -> {
//                resources.put(key, AsyncResourceSpecification.fromSync(resource));
//            });
//
//            Map<String, AsyncPromptSpecification> prompts = new HashMap<>();
//            syncSpec.prompts().forEach((key, prompt) -> {
//                prompts.put(key, AsyncPromptSpecification.fromSync(prompt));
//            });
//
//            Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions = new HashMap<>();
//            syncSpec.completions().forEach((key, completion) -> {
//                completions.put(key, AsyncCompletionSpecification.fromSync(completion));
//            });
//
//            List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers = new ArrayList<>();
//
//            for (var rootChangeConsumer : syncSpec.rootsChangeConsumers()) {
//                rootChangeConsumers.add((exchange, list) -> Mono
//                        .<Void>fromRunnable(() -> rootChangeConsumer.accept(new McpSyncServerExchange(exchange), list))
//                        .subscribeOn(Schedulers.boundedElastic()));
//            }
//
//            return new McpServerFeatures.Async(syncSpec.serverInfo(), syncSpec.serverCapabilities(), tools, resources,
//                    syncSpec.resourceTemplates(), prompts, completions, rootChangeConsumers, syncSpec.instructions());
//        }
//    }
//
//    /**
//     * Synchronous server features specification.
//     *
//     * @param serverInfo The server implementation details
//     * @param serverCapabilities The server capabilities
//     * @param tools The list of tool specifications
//     * @param resources The map of resource specifications
//     * @param resourceTemplates The list of resource templates
//     * @param prompts The map of prompt specifications
//     * @param rootsChangeConsumers The list of consumers that will be notified when the
//     * roots list changes
//     * @param instructions The server instructions text
//     */
//    record Sync(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
//                List<McpServerFeatures.SyncToolSpecification> tools,
//                Map<String, McpServerFeatures.SyncResourceSpecification> resources,
//                List<McpSchema.ResourceTemplate> resourceTemplates,
//                Map<String, McpServerFeatures.SyncPromptSpecification> prompts,
//                Map<McpSchema.CompleteReference, McpServerFeatures.SyncCompletionSpecification> completions,
//                List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers, String instructions) {
//
//        /**
//         * Create an instance and validate the arguments.
//         * @param serverInfo The server implementation details
//         * @param serverCapabilities The server capabilities
//         * @param tools The list of tool specifications
//         * @param resources The map of resource specifications
//         * @param resourceTemplates The list of resource templates
//         * @param prompts The map of prompt specifications
//         * @param rootsChangeConsumers The list of consumers that will be notified when
//         * the roots list changes
//         * @param instructions The server instructions text
//         */
//        Sync(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
//             List<McpServerFeatures.SyncToolSpecification> tools,
//             Map<String, McpServerFeatures.SyncResourceSpecification> resources,
//             List<McpSchema.ResourceTemplate> resourceTemplates,
//             Map<String, McpServerFeatures.SyncPromptSpecification> prompts,
//             Map<McpSchema.CompleteReference, McpServerFeatures.SyncCompletionSpecification> completions,
//             List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers,
//             String instructions) {
//
//            Assert.notNull(serverInfo, "Server info must not be null");
//
//            this.serverInfo = serverInfo;
//            this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
//                    : new McpSchema.ServerCapabilities(null, // completions
//                    null, // experimental
//                    new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
//                    // logging
//                    // by
//                    // default
//                    !Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
//                    !Utils.isEmpty(resources)
//                            ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
//                    !Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);
//
//            this.tools = (tools != null) ? tools : new ArrayList<>();
//            this.resources = (resources != null) ? resources : new HashMap<>();
//            this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : new ArrayList<>();
//            this.prompts = (prompts != null) ? prompts : new HashMap<>();
//            this.completions = (completions != null) ? completions : new HashMap<>();
//            this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : new ArrayList<>();
//            this.instructions = instructions;
//        }
//
//    }
//
//    public record SyncToolSpecification(CustomMcpSchema.Tool tool,
//                                        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
//    }
//
//    public record AsyncToolSpecification(CustomMcpSchema.Tool tool,
//                                         BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
//        static CustomMcpServerFeatures.AsyncToolSpecification fromSync(CustomMcpServerFeatures.SyncToolSpecification tool) {
//            // FIXME: This is temporary, proper validation should be implemented
//            if (tool == null) {
//                return null;
//            }
//            return new CustomMcpServerFeatures.AsyncToolSpecification(tool.tool(),
//                    (exchange, map) -> Mono
//                            .fromCallable(() -> tool.call().apply(new McpSyncServerExchange(exchange), map))
//                            .subscribeOn(Schedulers.boundedElastic()));
//        }
//    }
//
//
//}
