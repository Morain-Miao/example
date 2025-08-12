package custom;

import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.ai.mcp.server.autoconfigure.McpWebMvcServerAutoConfiguration;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogAccessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.web.context.support.StandardServletEnvironment;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@AutoConfiguration(after = {McpWebFluxServerAutoConfiguration.class })
@ConditionalOnClass({ McpSchema.class, McpSyncServer.class })
@EnableConfigurationProperties(McpServerProperties.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class ReturnDirectMcpServerAutoConfiguration {

    private static final LogAccessor logger = new LogAccessor(McpServerAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public McpServerTransportProvider stdioServerTransport() {
        return new StdioServerTransportProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public McpSchema.ServerCapabilities.Builder capabilitiesBuilder() {
        return McpSchema.ServerCapabilities.builder();
    }

    @Bean
    @ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
            matchIfMissing = true)
    public List<McpServerFeatures.SyncToolSpecification> syncTools(ObjectProvider<List<ToolCallback>> toolCalls,
                                                                   List<ToolCallback> toolCallbacksList, McpServerProperties serverProperties) {

        List<ToolCallback> tools = new ArrayList<>(toolCalls.stream().flatMap(List::stream).toList());

        if (!CollectionUtils.isEmpty(toolCallbacksList)) {
            tools.addAll(toolCallbacksList);
        }

        return this.toSyncToolSpecifications(tools, serverProperties);
    }

    private List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecifications(List<ToolCallback> tools,
                                                                                   McpServerProperties serverProperties) {

        // De-duplicate tools by their name, keeping the first occurrence of each tool
        // name
        return tools.stream() // Key: tool name
                .collect(Collectors.toMap(tool -> tool.getToolDefinition().name(), tool -> tool, // Value:
                        // the
                        // tool
                        // itself
                        (existing, replacement) -> existing)) // On duplicate key, keep the
                // existing tool
                .values()
                .stream()
                .map(tool -> {
                    String toolName = tool.getToolDefinition().name();
                    MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
                            ? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
                    return McpToolUtils.toSyncToolSpecification(tool, mimeType);
                })
                .toList();
    }

    @Bean
    @ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "SYNC",
            matchIfMissing = true)
    public McpSyncServer mcpSyncServer(McpServerTransportProvider transportProvider,
                                       McpSchema.ServerCapabilities.Builder capabilitiesBuilder, McpServerProperties serverProperties,
                                       ObjectProvider<List<McpServerFeatures.SyncToolSpecification>> tools,
                                       ObjectProvider<List<McpServerFeatures.SyncResourceSpecification>> resources,
                                       ObjectProvider<List<McpServerFeatures.SyncPromptSpecification>> prompts,
                                       ObjectProvider<List<McpServerFeatures.SyncCompletionSpecification>> completions,
                                       ObjectProvider<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers,
                                       List<ToolCallbackProvider> toolCallbackProvider, Environment environment) {

        McpSchema.Implementation serverInfo = new McpSchema.Implementation(serverProperties.getName(),
                serverProperties.getVersion());

        // Create the server with both tool and resource capabilities
        McpServer.SyncSpecification serverBuilder = McpServer.sync(transportProvider).serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            logger.info("Enable tools capabilities, notification: " + serverProperties.isToolChangeNotification());
            capabilitiesBuilder.tools(serverProperties.isToolChangeNotification());

            List<McpServerFeatures.SyncToolSpecification> toolSpecifications = new ArrayList<>(
                    tools.stream().flatMap(List::stream).toList());

            List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
                    .map(pr -> List.of(pr.getToolCallbacks()))
                    .flatMap(List::stream)
                    .filter(fc -> fc instanceof ToolCallback)
                    .map(fc -> (ToolCallback) fc)
                    .toList();

            toolSpecifications.addAll(this.toSyncToolSpecifications(providerToolCallbacks, serverProperties));

            if (!CollectionUtils.isEmpty(toolSpecifications)) {
                serverBuilder.tools(toolSpecifications);
                logger.info("Registered tools: " + toolSpecifications.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            logger.info(
                    "Enable resources capabilities, notification: " + serverProperties.isResourceChangeNotification());
            capabilitiesBuilder.resources(false, serverProperties.isResourceChangeNotification());

            List<McpServerFeatures.SyncResourceSpecification> resourceSpecifications = resources.stream().flatMap(List::stream).toList();
            if (!CollectionUtils.isEmpty(resourceSpecifications)) {
                serverBuilder.resources(resourceSpecifications);
                logger.info("Registered resources: " + resourceSpecifications.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            logger.info("Enable prompts capabilities, notification: " + serverProperties.isPromptChangeNotification());
            capabilitiesBuilder.prompts(serverProperties.isPromptChangeNotification());

            List<McpServerFeatures.SyncPromptSpecification> promptSpecifications = prompts.stream().flatMap(List::stream).toList();
            if (!CollectionUtils.isEmpty(promptSpecifications)) {
                serverBuilder.prompts(promptSpecifications);
                logger.info("Registered prompts: " + promptSpecifications.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("Enable completions capabilities");
            capabilitiesBuilder.completions();

            List<McpServerFeatures.SyncCompletionSpecification> completionSpecifications = completions.stream()
                    .flatMap(List::stream)
                    .toList();
            if (!CollectionUtils.isEmpty(completionSpecifications)) {
                serverBuilder.completions(completionSpecifications);
                logger.info("Registered completions: " + completionSpecifications.size());
            }
        }

        rootsChangeConsumers.ifAvailable(consumer -> {
            BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> syncConsumer = (exchange, roots) -> consumer
                    .accept(exchange, roots);
            serverBuilder.rootsChangeHandler(syncConsumer);
            logger.info("Registered roots change consumer");
        });

        serverBuilder.capabilities(capabilitiesBuilder.build());

        serverBuilder.instructions(serverProperties.getInstructions());

        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());
        if (environment instanceof StandardServletEnvironment) {
            serverBuilder.immediateExecution(true);
        }

        return serverBuilder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
    public List<McpServerFeatures.AsyncToolSpecification> asyncTools(ObjectProvider<List<ToolCallback>> toolCalls,
                                                                     List<ToolCallback> toolCallbackList, McpServerProperties serverProperties) {

        List<ToolCallback> tools = new ArrayList<>(toolCalls.stream().flatMap(List::stream).toList());
        if (!CollectionUtils.isEmpty(toolCallbackList)) {
            tools.addAll(toolCallbackList);
        }

        return this.toAsyncToolSpecification(tools, serverProperties);
    }

    private List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecification(List<ToolCallback> tools,
                                                                                    McpServerProperties serverProperties) {
        // De-duplicate tools by their name, keeping the first occurrence of each tool
        // name
        return tools.stream() // Key: tool name
                .collect(Collectors.toMap(tool -> tool.getToolDefinition().name(), tool -> tool, // Value:
                        // the
                        // tool
                        // itself
                        (existing, replacement) -> existing)) // On duplicate key, keep the
                // existing tool
                .values()
                .stream()
                .map(tool -> {
                    String toolName = tool.getToolDefinition().name();
                    MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
                            ? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
                    return McpToolUtils.toAsyncToolSpecification(tool, mimeType);
                })
                .toList();
    }

    @Bean
    @ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "type", havingValue = "ASYNC")
    public McpAsyncServer mcpAsyncServer(McpServerTransportProvider transportProvider,
                                         McpSchema.ServerCapabilities.Builder capabilitiesBuilder, McpServerProperties serverProperties,
                                         ObjectProvider<List<McpServerFeatures.AsyncToolSpecification>> tools,
                                         ObjectProvider<List<McpServerFeatures.AsyncResourceSpecification>> resources,
                                         ObjectProvider<List<McpServerFeatures.AsyncPromptSpecification>> prompts,
                                         ObjectProvider<List<McpServerFeatures.AsyncCompletionSpecification>> completions,
                                         ObjectProvider<BiConsumer<McpAsyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumer,
                                         List<ToolCallbackProvider> toolCallbackProvider) {

        McpSchema.Implementation serverInfo = new McpSchema.Implementation(serverProperties.getName(),
                serverProperties.getVersion());

        // Create the server with both tool and resource capabilities
        McpServer.AsyncSpecification serverBuilder = McpServer.async(transportProvider).serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = new ArrayList<>(
                    tools.stream().flatMap(List::stream).toList());
            List<ToolCallback> providerToolCallbacks = toolCallbackProvider.stream()
                    .map(pr -> List.of(pr.getToolCallbacks()))
                    .flatMap(List::stream)
                    .filter(fc -> fc instanceof ToolCallback)
                    .map(fc -> (ToolCallback) fc)
                    .toList();

            toolSpecifications.addAll(this.toAsyncToolSpecification(providerToolCallbacks, serverProperties));

            logger.info("Enable tools capabilities, notification: " + serverProperties.isToolChangeNotification());
            capabilitiesBuilder.tools(serverProperties.isToolChangeNotification());

            if (!CollectionUtils.isEmpty(toolSpecifications)) {
                serverBuilder.tools(toolSpecifications);
                logger.info("Registered tools: " + toolSpecifications.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            logger.info(
                    "Enable resources capabilities, notification: " + serverProperties.isResourceChangeNotification());
            capabilitiesBuilder.resources(false, serverProperties.isResourceChangeNotification());

            List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications = resources.stream().flatMap(List::stream).toList();
            if (!CollectionUtils.isEmpty(resourceSpecifications)) {
                serverBuilder.resources(resourceSpecifications);
                logger.info("Registered resources: " + resourceSpecifications.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            logger.info("Enable prompts capabilities, notification: " + serverProperties.isPromptChangeNotification());
            capabilitiesBuilder.prompts(serverProperties.isPromptChangeNotification());
            List<McpServerFeatures.AsyncPromptSpecification> promptSpecifications = prompts.stream().flatMap(List::stream).toList();

            if (!CollectionUtils.isEmpty(promptSpecifications)) {
                serverBuilder.prompts(promptSpecifications);
                logger.info("Registered prompts: " + promptSpecifications.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("Enable completions capabilities");
            capabilitiesBuilder.completions();
            List<McpServerFeatures.AsyncCompletionSpecification> completionSpecifications = completions.stream()
                    .flatMap(List::stream)
                    .toList();

            if (!CollectionUtils.isEmpty(completionSpecifications)) {
                serverBuilder.completions(completionSpecifications);
                logger.info("Registered completions: " + completionSpecifications.size());
            }
        }

        rootsChangeConsumer.ifAvailable(consumer -> {
            BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>> asyncConsumer = (exchange, roots) -> {
                consumer.accept(exchange, roots);
                return Mono.empty();
            };
            serverBuilder.rootsChangeHandler(asyncConsumer);
            logger.info("Registered roots change consumer");
        });

        serverBuilder.capabilities(capabilitiesBuilder.build());

        serverBuilder.instructions(serverProperties.getInstructions());

        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());

        return serverBuilder.build();
    }
}
