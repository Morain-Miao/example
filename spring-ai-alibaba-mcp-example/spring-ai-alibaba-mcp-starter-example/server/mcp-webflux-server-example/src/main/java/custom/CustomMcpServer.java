//package custom;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.modelcontextprotocol.server.*;
//import io.modelcontextprotocol.spec.McpSchema;
//import io.modelcontextprotocol.spec.McpServerTransportProvider;
//import io.modelcontextprotocol.util.Assert;
//import io.modelcontextprotocol.util.DeafaultMcpUriTemplateManagerFactory;
//import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.time.Duration;
//import java.util.*;
//import java.util.function.BiConsumer;
//import java.util.function.BiFunction;
//
//public interface CustomMcpServer extends McpServer {
//
//    /**
//     * Starts building a synchronous MCP server that provides blocking operations.
//     * Synchronous servers block the current Thread's execution upon each request before
//     * giving the control back to the caller, making them simpler to implement but
//     * potentially less scalable for concurrent operations.
//     * @param transportProvider The transport layer implementation for MCP communication.
//     * @return A new instance of {@link McpServer.SyncSpecification} for configuring the server.
//     */
//    static CustomMcpServer.SyncSpecification sync(McpServerTransportProvider transportProvider) {
//        return new CustomMcpServer.SyncSpecification(transportProvider);
//    }
//
//    /**
//     * Starts building an asynchronous MCP server that provides non-blocking operations.
//     * Asynchronous servers can handle multiple requests concurrently on a single Thread
//     * using a functional paradigm with non-blocking server transports, making them more
//     * scalable for high-concurrency scenarios but more complex to implement.
//     * @param transportProvider The transport layer implementation for MCP communication.
//     * @return A new instance of {@link McpServer.AsyncSpecification} for configuring the server.
//     */
//    static CustomMcpServer.AsyncSpecification async(McpServerTransportProvider transportProvider) {
//        return new CustomMcpServer.AsyncSpecification(transportProvider);
//    }
//
//    /**
//     * Asynchronous server specification.
//     */
//    class AsyncSpecification {
//
//        private static final McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server",
//                "1.0.0");
//
//        private final McpServerTransportProvider transportProvider;
//
//        private McpUriTemplateManagerFactory uriTemplateManagerFactory = new DeafaultMcpUriTemplateManagerFactory();
//
//        private ObjectMapper objectMapper;
//
//        private McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;
//
//        private McpSchema.ServerCapabilities serverCapabilities;
//
//        private String instructions;
//
//        /**
//         * The Model Context Protocol (MCP) allows servers to expose tools that can be
//         * invoked by language models. Tools enable models to interact with external
//         * systems, such as querying databases, calling APIs, or performing computations.
//         * Each tool is uniquely identified by a name and includes metadata describing its
//         * schema.
//         */
//        private final List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
//
//        /**
//         * The Model Context Protocol (MCP) provides a standardized way for servers to
//         * expose resources to clients. Resources allow servers to share data that
//         * provides context to language models, such as files, database schemas, or
//         * application-specific information. Each resource is uniquely identified by a
//         * URI.
//         */
//        private final Map<String, McpServerFeatures.AsyncResourceSpecification> resources = new HashMap<>();
//
//        private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();
//
//        /**
//         * The Model Context Protocol (MCP) provides a standardized way for servers to
//         * expose prompt templates to clients. Prompts allow servers to provide structured
//         * messages and instructions for interacting with language models. Clients can
//         * discover available prompts, retrieve their contents, and provide arguments to
//         * customize them.
//         */
//        private final Map<String, McpServerFeatures.AsyncPromptSpecification> prompts = new HashMap<>();
//
//        private final Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions = new HashMap<>();
//
//        private final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeHandlers = new ArrayList<>();
//
//        private Duration requestTimeout = Duration.ofSeconds(10); // Default timeout
//
//        private AsyncSpecification(McpServerTransportProvider transportProvider) {
//            Assert.notNull(transportProvider, "Transport provider must not be null");
//            this.transportProvider = transportProvider;
//        }
//
//        /**
//         * Sets the URI template manager factory to use for creating URI templates. This
//         * allows for custom URI template parsing and variable extraction.
//         * @param uriTemplateManagerFactory The factory to use. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if uriTemplateManagerFactory is null
//         */
//        public CustomMcpServer.AsyncSpecification uriTemplateManagerFactory(McpUriTemplateManagerFactory uriTemplateManagerFactory) {
//            Assert.notNull(uriTemplateManagerFactory, "URI template manager factory must not be null");
//            this.uriTemplateManagerFactory = uriTemplateManagerFactory;
//            return this;
//        }
//
//        /**
//         * Sets the duration to wait for server responses before timing out requests. This
//         * timeout applies to all requests made through the client, including tool calls,
//         * resource access, and prompt operations.
//         * @param requestTimeout The duration to wait before timing out requests. Must not
//         * be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if requestTimeout is null
//         */
//        public CustomMcpServer.AsyncSpecification requestTimeout(Duration requestTimeout) {
//            Assert.notNull(requestTimeout, "Request timeout must not be null");
//            this.requestTimeout = requestTimeout;
//            return this;
//        }
//
//        /**
//         * Sets the server implementation information that will be shared with clients
//         * during connection initialization. This helps with version compatibility,
//         * debugging, and server identification.
//         * @param serverInfo The server implementation details including name and version.
//         * Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if serverInfo is null
//         */
//        public CustomMcpServer.AsyncSpecification serverInfo(McpSchema.Implementation serverInfo) {
//            Assert.notNull(serverInfo, "Server info must not be null");
//            this.serverInfo = serverInfo;
//            return this;
//        }
//
//        /**
//         * Sets the server implementation information using name and version strings. This
//         * is a convenience method alternative to
//         * {@link #serverInfo(McpSchema.Implementation)}.
//         * @param name The server name. Must not be null or empty.
//         * @param version The server version. Must not be null or empty.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if name or version is null or empty
//         * @see #serverInfo(McpSchema.Implementation)
//         */
//        public CustomMcpServer.AsyncSpecification serverInfo(String name, String version) {
//            Assert.hasText(name, "Name must not be null or empty");
//            Assert.hasText(version, "Version must not be null or empty");
//            this.serverInfo = new McpSchema.Implementation(name, version);
//            return this;
//        }
//
//        /**
//         * Sets the server instructions that will be shared with clients during connection
//         * initialization. These instructions provide guidance to the client on how to
//         * interact with this server.
//         * @param instructions The instructions text. Can be null or empty.
//         * @return This builder instance for method chaining
//         */
//        public CustomMcpServer.AsyncSpecification instructions(String instructions) {
//            this.instructions = instructions;
//            return this;
//        }
//
//        /**
//         * Sets the server capabilities that will be advertised to clients during
//         * connection initialization. Capabilities define what features the server
//         * supports, such as:
//         * <ul>
//         * <li>Tool execution
//         * <li>Resource access
//         * <li>Prompt handling
//         * </ul>
//         * @param serverCapabilities The server capabilities configuration. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if serverCapabilities is null
//         */
//        public CustomMcpServer.AsyncSpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
//            Assert.notNull(serverCapabilities, "Server capabilities must not be null");
//            this.serverCapabilities = serverCapabilities;
//            return this;
//        }
//
//        /**
//         * Adds a single tool with its implementation handler to the server. This is a
//         * convenience method for registering individual tools without creating a
//         * {@link McpServerFeatures.AsyncToolSpecification} explicitly.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .tool(
//         *     new Tool("calculator", "Performs calculations", schema),
//         *     (exchange, args) -> Mono.fromSupplier(() -> calculate(args))
//         *         .map(result -> new CallToolResult("Result: " + result))
//         * )
//         * }</pre>
//         * @param tool The tool definition including name, description, and schema. Must
//         * not be null.
//         * @param handler The function that implements the tool's logic. Must not be null.
//         * The function's first argument is an {@link McpAsyncServerExchange} upon which
//         * the server can interact with the connected client. The second argument is the
//         * map of arguments passed to the tool.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if tool or handler is null
//         */
//        public CustomMcpServer.AsyncSpecification tool(McpSchema.Tool tool,
//                                                 BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> handler) {
//            Assert.notNull(tool, "Tool must not be null");
//            Assert.notNull(handler, "Handler must not be null");
//
//            this.tools.add(new McpServerFeatures.AsyncToolSpecification(tool, handler));
//
//            return this;
//        }
//
//        /**
//         * Adds multiple tools with their handlers to the server using a List. This method
//         * is useful when tools are dynamically generated or loaded from a configuration
//         * source.
//         * @param toolSpecifications The list of tool specifications to add. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if toolSpecifications is null
//         * @see #tools(McpServerFeatures.AsyncToolSpecification...)
//         */
//        public CustomMcpServer.AsyncSpecification tools(List<McpServerFeatures.AsyncToolSpecification> toolSpecifications) {
//            Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
//            this.tools.addAll(toolSpecifications);
//            return this;
//        }
//
//        /**
//         * Adds multiple tools with their handlers to the server using varargs. This
//         * method provides a convenient way to register multiple tools inline.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .tools(
//         *     new McpServerFeatures.AsyncToolSpecification(calculatorTool, calculatorHandler),
//         *     new McpServerFeatures.AsyncToolSpecification(weatherTool, weatherHandler),
//         *     new McpServerFeatures.AsyncToolSpecification(fileManagerTool, fileManagerHandler)
//         * )
//         * }</pre>
//         * @param toolSpecifications The tool specifications to add. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if toolSpecifications is null
//         * @see #tools(List)
//         */
//        public CustomMcpServer.AsyncSpecification tools(McpServerFeatures.AsyncToolSpecification... toolSpecifications) {
//            Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
//            for (McpServerFeatures.AsyncToolSpecification tool : toolSpecifications) {
//                this.tools.add(tool);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple resources with their handlers using a Map. This method is
//         * useful when resources are dynamically generated or loaded from a configuration
//         * source.
//         * @param resourceSpecifications Map of resource name to specification. Must not
//         * be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceSpecifications is null
//         * @see #resources(McpServerFeatures.AsyncResourceSpecification...)
//         */
//        public CustomMcpServer.AsyncSpecification resources(
//                Map<String, McpServerFeatures.AsyncResourceSpecification> resourceSpecifications) {
//            Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
//            this.resources.putAll(resourceSpecifications);
//            return this;
//        }
//
//        /**
//         * Registers multiple resources with their handlers using a List. This method is
//         * useful when resources need to be added in bulk from a collection.
//         * @param resourceSpecifications List of resource specifications. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceSpecifications is null
//         * @see #resources(McpServerFeatures.AsyncResourceSpecification...)
//         */
//        public CustomMcpServer.AsyncSpecification resources(List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications) {
//            Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
//            for (McpServerFeatures.AsyncResourceSpecification resource : resourceSpecifications) {
//                this.resources.put(resource.resource().uri(), resource);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple resources with their handlers using varargs. This method
//         * provides a convenient way to register multiple resources inline.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .resources(
//         *     new McpServerFeatures.AsyncResourceSpecification(fileResource, fileHandler),
//         *     new McpServerFeatures.AsyncResourceSpecification(dbResource, dbHandler),
//         *     new McpServerFeatures.AsyncResourceSpecification(apiResource, apiHandler)
//         * )
//         * }</pre>
//         * @param resourceSpecifications The resource specifications to add. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceSpecifications is null
//         */
//        public CustomMcpServer.AsyncSpecification resources(McpServerFeatures.AsyncResourceSpecification... resourceSpecifications) {
//            Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
//            for (McpServerFeatures.AsyncResourceSpecification resource : resourceSpecifications) {
//                this.resources.put(resource.resource().uri(), resource);
//            }
//            return this;
//        }
//
//        /**
//         * Sets the resource templates that define patterns for dynamic resource access.
//         * Templates use URI patterns with placeholders that can be filled at runtime.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .resourceTemplates(
//         *     new ResourceTemplate("file://{path}", "Access files by path"),
//         *     new ResourceTemplate("db://{table}/{id}", "Access database records")
//         * )
//         * }</pre>
//         * @param resourceTemplates List of resource templates. If null, clears existing
//         * templates.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceTemplates is null.
//         * @see #resourceTemplates(McpSchema.ResourceTemplate...)
//         */
//        public CustomMcpServer.AsyncSpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
//            Assert.notNull(resourceTemplates, "Resource templates must not be null");
//            this.resourceTemplates.addAll(resourceTemplates);
//            return this;
//        }
//
//        /**
//         * Sets the resource templates using varargs for convenience. This is an
//         * alternative to {@link #resourceTemplates(List)}.
//         * @param resourceTemplates The resource templates to set.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceTemplates is null.
//         * @see #resourceTemplates(List)
//         */
//        public CustomMcpServer.AsyncSpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
//            Assert.notNull(resourceTemplates, "Resource templates must not be null");
//            for (McpSchema.ResourceTemplate resourceTemplate : resourceTemplates) {
//                this.resourceTemplates.add(resourceTemplate);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple prompts with their handlers using a Map. This method is
//         * useful when prompts are dynamically generated or loaded from a configuration
//         * source.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .prompts(Map.of("analysis", new McpServerFeatures.AsyncPromptSpecification(
//         *     new Prompt("analysis", "Code analysis template"),
//         *     request -> Mono.fromSupplier(() -> generateAnalysisPrompt(request))
//         *         .map(GetPromptResult::new)
//         * )));
//         * }</pre>
//         * @param prompts Map of prompt name to specification. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if prompts is null
//         */
//        public CustomMcpServer.AsyncSpecification prompts(Map<String, McpServerFeatures.AsyncPromptSpecification> prompts) {
//            Assert.notNull(prompts, "Prompts map must not be null");
//            this.prompts.putAll(prompts);
//            return this;
//        }
//
//        /**
//         * Registers multiple prompts with their handlers using a List. This method is
//         * useful when prompts need to be added in bulk from a collection.
//         * @param prompts List of prompt specifications. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if prompts is null
//         * @see #prompts(McpServerFeatures.AsyncPromptSpecification...)
//         */
//        public CustomMcpServer.AsyncSpecification prompts(List<McpServerFeatures.AsyncPromptSpecification> prompts) {
//            Assert.notNull(prompts, "Prompts list must not be null");
//            for (McpServerFeatures.AsyncPromptSpecification prompt : prompts) {
//                this.prompts.put(prompt.prompt().name(), prompt);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple prompts with their handlers using varargs. This method
//         * provides a convenient way to register multiple prompts inline.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .prompts(
//         *     new McpServerFeatures.AsyncPromptSpecification(analysisPrompt, analysisHandler),
//         *     new McpServerFeatures.AsyncPromptSpecification(summaryPrompt, summaryHandler),
//         *     new McpServerFeatures.AsyncPromptSpecification(reviewPrompt, reviewHandler)
//         * )
//         * }</pre>
//         * @param prompts The prompt specifications to add. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if prompts is null
//         */
//        public CustomMcpServer.AsyncSpecification prompts(McpServerFeatures.AsyncPromptSpecification... prompts) {
//            Assert.notNull(prompts, "Prompts list must not be null");
//            for (McpServerFeatures.AsyncPromptSpecification prompt : prompts) {
//                this.prompts.put(prompt.prompt().name(), prompt);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple completions with their handlers using a List. This method is
//         * useful when completions need to be added in bulk from a collection.
//         * @param completions List of completion specifications. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if completions is null
//         */
//        public CustomMcpServer.AsyncSpecification completions(List<McpServerFeatures.AsyncCompletionSpecification> completions) {
//            Assert.notNull(completions, "Completions list must not be null");
//            for (McpServerFeatures.AsyncCompletionSpecification completion : completions) {
//                this.completions.put(completion.referenceKey(), completion);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple completions with their handlers using varargs. This method
//         * is useful when completions are defined inline and added directly.
//         * @param completions Array of completion specifications. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if completions is null
//         */
//        public CustomMcpServer.AsyncSpecification completions(McpServerFeatures.AsyncCompletionSpecification... completions) {
//            Assert.notNull(completions, "Completions list must not be null");
//            for (McpServerFeatures.AsyncCompletionSpecification completion : completions) {
//                this.completions.put(completion.referenceKey(), completion);
//            }
//            return this;
//        }
//
//        /**
//         * Registers a consumer that will be notified when the list of roots changes. This
//         * is useful for updating resource availability dynamically, such as when new
//         * files are added or removed.
//         * @param handler The handler to register. Must not be null. The function's first
//         * argument is an {@link McpAsyncServerExchange} upon which the server can
//         * interact with the connected client. The second argument is the list of roots.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if consumer is null
//         */
//        public CustomMcpServer.AsyncSpecification rootsChangeHandler(
//                BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>> handler) {
//            Assert.notNull(handler, "Consumer must not be null");
//            this.rootsChangeHandlers.add(handler);
//            return this;
//        }
//
//        /**
//         * Registers multiple consumers that will be notified when the list of roots
//         * changes. This method is useful when multiple consumers need to be registered at
//         * once.
//         * @param handlers The list of handlers to register. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if consumers is null
//         * @see #rootsChangeHandler(BiFunction)
//         */
//        public CustomMcpServer.AsyncSpecification rootsChangeHandlers(
//                List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> handlers) {
//            Assert.notNull(handlers, "Handlers list must not be null");
//            this.rootsChangeHandlers.addAll(handlers);
//            return this;
//        }
//
//        /**
//         * Registers multiple consumers that will be notified when the list of roots
//         * changes using varargs. This method provides a convenient way to register
//         * multiple consumers inline.
//         * @param handlers The handlers to register. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if consumers is null
//         * @see #rootsChangeHandlers(List)
//         */
//        public CustomMcpServer.AsyncSpecification rootsChangeHandlers(
//                @SuppressWarnings("unchecked") BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>... handlers) {
//            Assert.notNull(handlers, "Handlers list must not be null");
//            return this.rootsChangeHandlers(Arrays.asList(handlers));
//        }
//
//        /**
//         * Sets the object mapper to use for serializing and deserializing JSON messages.
//         * @param objectMapper the instance to use. Must not be null.
//         * @return This builder instance for method chaining.
//         * @throws IllegalArgumentException if objectMapper is null
//         */
//        public CustomMcpServer.AsyncSpecification objectMapper(ObjectMapper objectMapper) {
//            Assert.notNull(objectMapper, "ObjectMapper must not be null");
//            this.objectMapper = objectMapper;
//            return this;
//        }
//
//        /**
//         * Builds an asynchronous MCP server that provides non-blocking operations.
//         * @return A new instance of {@link McpAsyncServer} configured with this builder's
//         * settings.
//         */
//        public CustomMcpServer build() {
//            var features = new McpServerFeatures.Async(this.serverInfo, this.serverCapabilities, this.tools,
//                    this.resources, this.resourceTemplates, this.prompts, this.completions, this.rootsChangeHandlers,
//                    this.instructions);
//            var mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
//            return new McpAsyncServer(this.transportProvider, mapper, features, this.requestTimeout,
//                    this.uriTemplateManagerFactory);
//        }
//
//    }
//
//    /**
//     * Synchronous server specification.
//     */
//    class SyncSpecification {
//
//        private static final McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server",
//                "1.0.0");
//
//        private McpUriTemplateManagerFactory uriTemplateManagerFactory = new DeafaultMcpUriTemplateManagerFactory();
//
//        private final McpServerTransportProvider transportProvider;
//
//        private ObjectMapper objectMapper;
//
//        private McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;
//
//        private McpSchema.ServerCapabilities serverCapabilities;
//
//        private String instructions;
//
//        /**
//         * The Model Context Protocol (MCP) allows servers to expose tools that can be
//         * invoked by language models. Tools enable models to interact with external
//         * systems, such as querying databases, calling APIs, or performing computations.
//         * Each tool is uniquely identified by a name and includes metadata describing its
//         * schema.
//         */
//        private final List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();
//
//        /**
//         * The Model Context Protocol (MCP) provides a standardized way for servers to
//         * expose resources to clients. Resources allow servers to share data that
//         * provides context to language models, such as files, database schemas, or
//         * application-specific information. Each resource is uniquely identified by a
//         * URI.
//         */
//        private final Map<String, McpServerFeatures.SyncResourceSpecification> resources = new HashMap<>();
//
//        private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();
//
//        /**
//         * The Model Context Protocol (MCP) provides a standardized way for servers to
//         * expose prompt templates to clients. Prompts allow servers to provide structured
//         * messages and instructions for interacting with language models. Clients can
//         * discover available prompts, retrieve their contents, and provide arguments to
//         * customize them.
//         */
//        private final Map<String, McpServerFeatures.SyncPromptSpecification> prompts = new HashMap<>();
//
//        private final Map<McpSchema.CompleteReference, McpServerFeatures.SyncCompletionSpecification> completions = new HashMap<>();
//
//        private final List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeHandlers = new ArrayList<>();
//
//        private Duration requestTimeout = Duration.ofSeconds(10); // Default timeout
//
//        private SyncSpecification(McpServerTransportProvider transportProvider) {
//            Assert.notNull(transportProvider, "Transport provider must not be null");
//            this.transportProvider = transportProvider;
//        }
//
//        /**
//         * Sets the URI template manager factory to use for creating URI templates. This
//         * allows for custom URI template parsing and variable extraction.
//         * @param uriTemplateManagerFactory The factory to use. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if uriTemplateManagerFactory is null
//         */
//        public McpServer.SyncSpecification uriTemplateManagerFactory(McpUriTemplateManagerFactory uriTemplateManagerFactory) {
//            Assert.notNull(uriTemplateManagerFactory, "URI template manager factory must not be null");
//            this.uriTemplateManagerFactory = uriTemplateManagerFactory;
//            return this;
//        }
//
//        /**
//         * Sets the duration to wait for server responses before timing out requests. This
//         * timeout applies to all requests made through the client, including tool calls,
//         * resource access, and prompt operations.
//         * @param requestTimeout The duration to wait before timing out requests. Must not
//         * be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if requestTimeout is null
//         */
//        public CustomMcpServer.SyncSpecification requestTimeout(Duration requestTimeout) {
//            Assert.notNull(requestTimeout, "Request timeout must not be null");
//            this.requestTimeout = requestTimeout;
//            return this;
//        }
//
//        /**
//         * Sets the server implementation information that will be shared with clients
//         * during connection initialization. This helps with version compatibility,
//         * debugging, and server identification.
//         * @param serverInfo The server implementation details including name and version.
//         * Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if serverInfo is null
//         */
//        public CustomMcpServer.SyncSpecification serverInfo(McpSchema.Implementation serverInfo) {
//            Assert.notNull(serverInfo, "Server info must not be null");
//            this.serverInfo = serverInfo;
//            return this;
//        }
//
//        /**
//         * Sets the server implementation information using name and version strings. This
//         * is a convenience method alternative to
//         * {@link #serverInfo(McpSchema.Implementation)}.
//         * @param name The server name. Must not be null or empty.
//         * @param version The server version. Must not be null or empty.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if name or version is null or empty
//         * @see #serverInfo(McpSchema.Implementation)
//         */
//        public CustomMcpServer.SyncSpecification serverInfo(String name, String version) {
//            Assert.hasText(name, "Name must not be null or empty");
//            Assert.hasText(version, "Version must not be null or empty");
//            this.serverInfo = new McpSchema.Implementation(name, version);
//            return this;
//        }
//
//        /**
//         * Sets the server instructions that will be shared with clients during connection
//         * initialization. These instructions provide guidance to the client on how to
//         * interact with this server.
//         * @param instructions The instructions text. Can be null or empty.
//         * @return This builder instance for method chaining
//         */
//        public CustomMcpServer.SyncSpecification instructions(String instructions) {
//            this.instructions = instructions;
//            return this;
//        }
//
//        /**
//         * Sets the server capabilities that will be advertised to clients during
//         * connection initialization. Capabilities define what features the server
//         * supports, such as:
//         * <ul>
//         * <li>Tool execution
//         * <li>Resource access
//         * <li>Prompt handling
//         * </ul>
//         * @param serverCapabilities The server capabilities configuration. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if serverCapabilities is null
//         */
//        public CustomMcpServer.SyncSpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
//            Assert.notNull(serverCapabilities, "Server capabilities must not be null");
//            this.serverCapabilities = serverCapabilities;
//            return this;
//        }
//
//        /**
//         * Adds a single tool with its implementation handler to the server. This is a
//         * convenience method for registering individual tools without creating a
//         * {@link McpServerFeatures.SyncToolSpecification} explicitly.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .tool(
//         *     new Tool("calculator", "Performs calculations", schema),
//         *     (exchange, args) -> new CallToolResult("Result: " + calculate(args))
//         * )
//         * }</pre>
//         * @param tool The tool definition including name, description, and schema. Must
//         * not be null.
//         * @param handler The function that implements the tool's logic. Must not be null.
//         * The function's first argument is an {@link McpSyncServerExchange} upon which
//         * the server can interact with the connected client. The second argument is the
//         * list of arguments passed to the tool.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if tool or handler is null
//         */
//        public CustomMcpServer.SyncSpecification tool(McpSchema.Tool tool,
//                                                BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler) {
//            Assert.notNull(tool, "Tool must not be null");
//            Assert.notNull(handler, "Handler must not be null");
//
//            this.tools.add(new McpServerFeatures.SyncToolSpecification(tool, handler));
//
//            return this;
//        }
//
//        /**
//         * Adds multiple tools with their handlers to the server using a List. This method
//         * is useful when tools are dynamically generated or loaded from a configuration
//         * source.
//         * @param toolSpecifications The list of tool specifications to add. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if toolSpecifications is null
//         * @see #tools(McpServerFeatures.SyncToolSpecification...)
//         */
//        public CustomMcpServer.SyncSpecification tools(List<McpServerFeatures.SyncToolSpecification> toolSpecifications) {
//            Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
//            this.tools.addAll(toolSpecifications);
//            return this;
//        }
//
//        /**
//         * Adds multiple tools with their handlers to the server using varargs. This
//         * method provides a convenient way to register multiple tools inline.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .tools(
//         *     new ToolSpecification(calculatorTool, calculatorHandler),
//         *     new ToolSpecification(weatherTool, weatherHandler),
//         *     new ToolSpecification(fileManagerTool, fileManagerHandler)
//         * )
//         * }</pre>
//         * @param toolSpecifications The tool specifications to add. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if toolSpecifications is null
//         * @see #tools(List)
//         */
//        public CustomMcpServer.SyncSpecification tools(McpServerFeatures.SyncToolSpecification... toolSpecifications) {
//            Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
//            for (McpServerFeatures.SyncToolSpecification tool : toolSpecifications) {
//                this.tools.add(tool);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple resources with their handlers using a Map. This method is
//         * useful when resources are dynamically generated or loaded from a configuration
//         * source.
//         * @param resourceSpecifications Map of resource name to specification. Must not
//         * be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceSpecifications is null
//         * @see #resources(McpServerFeatures.SyncResourceSpecification...)
//         */
//        public CustomMcpServer.SyncSpecification resources(
//                Map<String, McpServerFeatures.SyncResourceSpecification> resourceSpecifications) {
//            Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
//            this.resources.putAll(resourceSpecifications);
//            return this;
//        }
//
//        /**
//         * Registers multiple resources with their handlers using a List. This method is
//         * useful when resources need to be added in bulk from a collection.
//         * @param resourceSpecifications List of resource specifications. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceSpecifications is null
//         * @see #resources(McpServerFeatures.SyncResourceSpecification...)
//         */
//        public CustomMcpServer.SyncSpecification resources(List<McpServerFeatures.SyncResourceSpecification> resourceSpecifications) {
//            Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
//            for (McpServerFeatures.SyncResourceSpecification resource : resourceSpecifications) {
//                this.resources.put(resource.resource().uri(), resource);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple resources with their handlers using varargs. This method
//         * provides a convenient way to register multiple resources inline.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .resources(
//         *     new ResourceSpecification(fileResource, fileHandler),
//         *     new ResourceSpecification(dbResource, dbHandler),
//         *     new ResourceSpecification(apiResource, apiHandler)
//         * )
//         * }</pre>
//         * @param resourceSpecifications The resource specifications to add. Must not be
//         * null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceSpecifications is null
//         */
//        public CustomMcpServer.SyncSpecification resources(McpServerFeatures.SyncResourceSpecification... resourceSpecifications) {
//            Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
//            for (McpServerFeatures.SyncResourceSpecification resource : resourceSpecifications) {
//                this.resources.put(resource.resource().uri(), resource);
//            }
//            return this;
//        }
//
//        /**
//         * Sets the resource templates that define patterns for dynamic resource access.
//         * Templates use URI patterns with placeholders that can be filled at runtime.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .resourceTemplates(
//         *     new ResourceTemplate("file://{path}", "Access files by path"),
//         *     new ResourceTemplate("db://{table}/{id}", "Access database records")
//         * )
//         * }</pre>
//         * @param resourceTemplates List of resource templates. If null, clears existing
//         * templates.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceTemplates is null.
//         * @see #resourceTemplates(McpSchema.ResourceTemplate...)
//         */
//        public CustomMcpServer.SyncSpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
//            Assert.notNull(resourceTemplates, "Resource templates must not be null");
//            this.resourceTemplates.addAll(resourceTemplates);
//            return this;
//        }
//
//        /**
//         * Sets the resource templates using varargs for convenience. This is an
//         * alternative to {@link #resourceTemplates(List)}.
//         * @param resourceTemplates The resource templates to set.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if resourceTemplates is null
//         * @see #resourceTemplates(List)
//         */
//        public CustomMcpServer.SyncSpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
//            Assert.notNull(resourceTemplates, "Resource templates must not be null");
//            for (McpSchema.ResourceTemplate resourceTemplate : resourceTemplates) {
//                this.resourceTemplates.add(resourceTemplate);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple prompts with their handlers using a Map. This method is
//         * useful when prompts are dynamically generated or loaded from a configuration
//         * source.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * Map<String, PromptSpecification> prompts = new HashMap<>();
//         * prompts.put("analysis", new PromptSpecification(
//         *     new Prompt("analysis", "Code analysis template"),
//         *     (exchange, request) -> new GetPromptResult(generateAnalysisPrompt(request))
//         * ));
//         * .prompts(prompts)
//         * }</pre>
//         * @param prompts Map of prompt name to specification. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if prompts is null
//         */
//        public CustomMcpServer.SyncSpecification prompts(Map<String, McpServerFeatures.SyncPromptSpecification> prompts) {
//            Assert.notNull(prompts, "Prompts map must not be null");
//            this.prompts.putAll(prompts);
//            return this;
//        }
//
//        /**
//         * Registers multiple prompts with their handlers using a List. This method is
//         * useful when prompts need to be added in bulk from a collection.
//         * @param prompts List of prompt specifications. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if prompts is null
//         * @see #prompts(McpServerFeatures.SyncPromptSpecification...)
//         */
//        public CustomMcpServer.SyncSpecification prompts(List<McpServerFeatures.SyncPromptSpecification> prompts) {
//            Assert.notNull(prompts, "Prompts list must not be null");
//            for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
//                this.prompts.put(prompt.prompt().name(), prompt);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple prompts with their handlers using varargs. This method
//         * provides a convenient way to register multiple prompts inline.
//         *
//         * <p>
//         * Example usage: <pre>{@code
//         * .prompts(
//         *     new PromptSpecification(analysisPrompt, analysisHandler),
//         *     new PromptSpecification(summaryPrompt, summaryHandler),
//         *     new PromptSpecification(reviewPrompt, reviewHandler)
//         * )
//         * }</pre>
//         * @param prompts The prompt specifications to add. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if prompts is null
//         */
//        public CustomMcpServer.SyncSpecification prompts(McpServerFeatures.SyncPromptSpecification... prompts) {
//            Assert.notNull(prompts, "Prompts list must not be null");
//            for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
//                this.prompts.put(prompt.prompt().name(), prompt);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple completions with their handlers using a List. This method is
//         * useful when completions need to be added in bulk from a collection.
//         * @param completions List of completion specifications. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if completions is null
//         * @see #completions(McpServerFeatures.SyncCompletionSpecification...)
//         */
//        public CustomMcpServer.SyncSpecification completions(List<McpServerFeatures.SyncCompletionSpecification> completions) {
//            Assert.notNull(completions, "Completions list must not be null");
//            for (McpServerFeatures.SyncCompletionSpecification completion : completions) {
//                this.completions.put(completion.referenceKey(), completion);
//            }
//            return this;
//        }
//
//        /**
//         * Registers multiple completions with their handlers using varargs. This method
//         * is useful when completions are defined inline and added directly.
//         * @param completions Array of completion specifications. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if completions is null
//         */
//        public CustomMcpServer.SyncSpecification completions(McpServerFeatures.SyncCompletionSpecification... completions) {
//            Assert.notNull(completions, "Completions list must not be null");
//            for (McpServerFeatures.SyncCompletionSpecification completion : completions) {
//                this.completions.put(completion.referenceKey(), completion);
//            }
//            return this;
//        }
//
//        /**
//         * Registers a consumer that will be notified when the list of roots changes. This
//         * is useful for updating resource availability dynamically, such as when new
//         * files are added or removed.
//         * @param handler The handler to register. Must not be null. The function's first
//         * argument is an {@link McpSyncServerExchange} upon which the server can interact
//         * with the connected client. The second argument is the list of roots.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if consumer is null
//         */
//        public CustomMcpServer.SyncSpecification rootsChangeHandler(BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> handler) {
//            Assert.notNull(handler, "Consumer must not be null");
//            this.rootsChangeHandlers.add(handler);
//            return this;
//        }
//
//        /**
//         * Registers multiple consumers that will be notified when the list of roots
//         * changes. This method is useful when multiple consumers need to be registered at
//         * once.
//         * @param handlers The list of handlers to register. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if consumers is null
//         * @see #rootsChangeHandler(BiConsumer)
//         */
//        public CustomMcpServer.SyncSpecification rootsChangeHandlers(
//                List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> handlers) {
//            Assert.notNull(handlers, "Handlers list must not be null");
//            this.rootsChangeHandlers.addAll(handlers);
//            return this;
//        }
//
//        /**
//         * Registers multiple consumers that will be notified when the list of roots
//         * changes using varargs. This method provides a convenient way to register
//         * multiple consumers inline.
//         * @param handlers The handlers to register. Must not be null.
//         * @return This builder instance for method chaining
//         * @throws IllegalArgumentException if consumers is null
//         * @see #rootsChangeHandlers(List)
//         */
//        public CustomMcpServer.SyncSpecification rootsChangeHandlers(
//                BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>... handlers) {
//            Assert.notNull(handlers, "Handlers list must not be null");
//            return this.rootsChangeHandlers(List.of(handlers));
//        }
//
//        /**
//         * Sets the object mapper to use for serializing and deserializing JSON messages.
//         * @param objectMapper the instance to use. Must not be null.
//         * @return This builder instance for method chaining.
//         * @throws IllegalArgumentException if objectMapper is null
//         */
//        public CustomMcpServer.SyncSpecification objectMapper(ObjectMapper objectMapper) {
//            Assert.notNull(objectMapper, "ObjectMapper must not be null");
//            this.objectMapper = objectMapper;
//            return this;
//        }
//
//        /**
//         * Builds a synchronous MCP server that provides blocking operations.
//         * @return A new instance of {@link McpSyncServer} configured with this builder's
//         * settings.
//         */
//        public McpSyncServer build() {
//            McpServerFeatures.Sync syncFeatures = new McpServerFeatures.Sync(this.serverInfo, this.serverCapabilities,
//                    this.tools, this.resources, this.resourceTemplates, this.prompts, this.completions,
//                    this.rootsChangeHandlers, this.instructions);
//            McpServerFeatures.Async asyncFeatures = McpServerFeatures.Async.fromSync(syncFeatures);
//            var mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
//            var asyncServer = new McpAsyncServer(this.transportProvider, mapper, asyncFeatures, this.requestTimeout,
//                    this.uriTemplateManagerFactory);
//
//            return new McpSyncServer(asyncServer);
//        }
//
//    }
//
//    public record AsyncToolSpecification(McpSchema.Tool tool,
//                                         BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
//
//        static McpServerFeatures.AsyncToolSpecification fromSync(McpServerFeatures.SyncToolSpecification tool) {
//            // FIXME: This is temporary, proper validation should be implemented
//            if (tool == null) {
//                return null;
//            }
//            return new McpServerFeatures.AsyncToolSpecification(tool.tool(),
//                    (exchange, map) -> Mono
//                            .fromCallable(() -> tool.call().apply(new McpSyncServerExchange(exchange), map))
//                            .subscribeOn(Schedulers.boundedElastic()));
//        }
//    }
//
//    /**
//     * Specification of a resource with its asynchronous handler function. Resources
//     * provide context to AI models by exposing data such as:
//     * <ul>
//     * <li>File contents
//     * <li>Database records
//     * <li>API responses
//     * <li>System information
//     * <li>Application state
//     * </ul>
//     *
//     * <p>
//     * Example resource specification: <pre>{@code
//     * new McpServerFeatures.AsyncResourceSpecification(
//     *     new Resource("docs", "Documentation files", "text/markdown"),
//     *     (exchange, request) ->
//     *         Mono.fromSupplier(() -> readFile(request.getPath()))
//     *             .map(ReadResourceResult::new)
//     * )
//     * }</pre>
//     *
//     * @param resource The resource definition including name, description, and MIME type
//     * @param readHandler The function that handles resource read requests. The function's
//     * first argument is an {@link McpAsyncServerExchange} upon which the server can
//     * interact with the connected client. The second arguments is a
//     * {@link io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest}.
//     */
//    public record AsyncResourceSpecification(McpSchema.Resource resource,
//                                             BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler) {
//
//        static McpServerFeatures.AsyncResourceSpecification fromSync(McpServerFeatures.SyncResourceSpecification resource) {
//            // FIXME: This is temporary, proper validation should be implemented
//            if (resource == null) {
//                return null;
//            }
//            return new McpServerFeatures.AsyncResourceSpecification(resource.resource(),
//                    (exchange, req) -> Mono
//                            .fromCallable(() -> resource.readHandler().apply(new McpSyncServerExchange(exchange), req))
//                            .subscribeOn(Schedulers.boundedElastic()));
//        }
//    }
//
//    /**
//     * Specification of a prompt template with its asynchronous handler function. Prompts
//     * provide structured templates for AI model interactions, supporting:
//     * <ul>
//     * <li>Consistent message formatting
//     * <li>Parameter substitution
//     * <li>Context injection
//     * <li>Response formatting
//     * <li>Instruction templating
//     * </ul>
//     *
//     * <p>
//     * Example prompt specification: <pre>{@code
//     * new McpServerFeatures.AsyncPromptSpecification(
//     *     new Prompt("analyze", "Code analysis template"),
//     *     (exchange, request) -> {
//     *         String code = request.getArguments().get("code");
//     *         return Mono.just(new GetPromptResult(
//     *             "Analyze this code:\n\n" + code + "\n\nProvide feedback on:"
//     *         ));
//     *     }
//     * )
//     * }</pre>
//     *
//     * @param prompt The prompt definition including name and description
//     * @param promptHandler The function that processes prompt requests and returns
//     * formatted templates. The function's first argument is an
//     * {@link McpAsyncServerExchange} upon which the server can interact with the
//     * connected client. The second arguments is a
//     * {@link io.modelcontextprotocol.spec.McpSchema.GetPromptRequest}.
//     */
//    public record AsyncPromptSpecification(McpSchema.Prompt prompt,
//                                           BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler) {
//
//        static McpServerFeatures.AsyncPromptSpecification fromSync(McpServerFeatures.SyncPromptSpecification prompt) {
//            // FIXME: This is temporary, proper validation should be implemented
//            if (prompt == null) {
//                return null;
//            }
//            return new McpServerFeatures.AsyncPromptSpecification(prompt.prompt(),
//                    (exchange, req) -> Mono
//                            .fromCallable(() -> prompt.promptHandler().apply(new McpSyncServerExchange(exchange), req))
//                            .subscribeOn(Schedulers.boundedElastic()));
//        }
//    }
//
//    /**
//     * Specification of a completion handler function with asynchronous execution support.
//     * Completions generate AI model outputs based on prompt or resource references and
//     * user-provided arguments. This abstraction enables:
//     * <ul>
//     * <li>Customizable response generation logic
//     * <li>Parameter-driven template expansion
//     * <li>Dynamic interaction with connected clients
//     * </ul>
//     *
//     * @param referenceKey The unique key representing the completion reference.
//     * @param completionHandler The asynchronous function that processes completion
//     * requests and returns results. The first argument is an
//     * {@link McpAsyncServerExchange} used to interact with the client. The second
//     * argument is a {@link io.modelcontextprotocol.spec.McpSchema.CompleteRequest}.
//     */
//    public record AsyncCompletionSpecification(McpSchema.CompleteReference referenceKey,
//                                               BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> completionHandler) {
//
//        /**
//         * Converts a synchronous {@link McpServerFeatures.SyncCompletionSpecification} into an
//         * {@link McpServerFeatures.AsyncCompletionSpecification} by wrapping the handler in a bounded
//         * elastic scheduler for safe non-blocking execution.
//         * @param completion the synchronous completion specification
//         * @return an asynchronous wrapper of the provided sync specification, or
//         * {@code null} if input is null
//         */
//        static McpServerFeatures.AsyncCompletionSpecification fromSync(McpServerFeatures.SyncCompletionSpecification completion) {
//            if (completion == null) {
//                return null;
//            }
//            return new McpServerFeatures.AsyncCompletionSpecification(completion.referenceKey(),
//                    (exchange, request) -> Mono.fromCallable(
//                                    () -> completion.completionHandler().apply(new McpSyncServerExchange(exchange), request))
//                            .subscribeOn(Schedulers.boundedElastic()));
//        }
//    }
//
//    /**
//     * Specification of a tool with its synchronous handler function. Tools are the
//     * primary way for MCP servers to expose functionality to AI models. Each tool
//     * represents a specific capability, such as:
//     * <ul>
//     * <li>Performing calculations
//     * <li>Accessing external APIs
//     * <li>Querying databases
//     * <li>Manipulating files
//     * <li>Executing system commands
//     * </ul>
//     *
//     * <p>
//     * Example tool specification: <pre>{@code
//     * new McpServerFeatures.SyncToolSpecification(
//     *     new Tool(
//     *         "calculator",
//     *         "Performs mathematical calculations",
//     *         new JsonSchemaObject()
//     *             .required("expression")
//     *             .property("expression", JsonSchemaType.STRING)
//     *     ),
//     *     (exchange, args) -> {
//     *         String expr = (String) args.get("expression");
//     *         return new CallToolResult("Result: " + evaluate(expr));
//     *     }
//     * )
//     * }</pre>
//     *
//     * @param tool The tool definition including name, description, and parameter schema
//     * @param call The function that implements the tool's logic, receiving arguments and
//     * returning results. The function's first argument is an
//     * {@link McpSyncServerExchange} upon which the server can interact with the connected
//     * client. The second arguments is a map of arguments passed to the tool.
//     */
//    public record SyncToolSpecification(McpSchema.Tool tool,
//                                        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
//    }
//
//    /**
//     * Specification of a resource with its synchronous handler function. Resources
//     * provide context to AI models by exposing data such as:
//     * <ul>
//     * <li>File contents
//     * <li>Database records
//     * <li>API responses
//     * <li>System information
//     * <li>Application state
//     * </ul>
//     *
//     * <p>
//     * Example resource specification: <pre>{@code
//     * new McpServerFeatures.SyncResourceSpecification(
//     *     new Resource("docs", "Documentation files", "text/markdown"),
//     *     (exchange, request) -> {
//     *         String content = readFile(request.getPath());
//     *         return new ReadResourceResult(content);
//     *     }
//     * )
//     * }</pre>
//     *
//     * @param resource The resource definition including name, description, and MIME type
//     * @param readHandler The function that handles resource read requests. The function's
//     * first argument is an {@link McpSyncServerExchange} upon which the server can
//     * interact with the connected client. The second arguments is a
//     * {@link io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest}.
//     */
//    public record SyncResourceSpecification(McpSchema.Resource resource,
//                                            BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler) {
//    }
//
//    /**
//     * Specification of a prompt template with its synchronous handler function. Prompts
//     * provide structured templates for AI model interactions, supporting:
//     * <ul>
//     * <li>Consistent message formatting
//     * <li>Parameter substitution
//     * <li>Context injection
//     * <li>Response formatting
//     * <li>Instruction templating
//     * </ul>
//     *
//     * <p>
//     * Example prompt specification: <pre>{@code
//     * new McpServerFeatures.SyncPromptSpecification(
//     *     new Prompt("analyze", "Code analysis template"),
//     *     (exchange, request) -> {
//     *         String code = request.getArguments().get("code");
//     *         return new GetPromptResult(
//     *             "Analyze this code:\n\n" + code + "\n\nProvide feedback on:"
//     *         );
//     *     }
//     * )
//     * }</pre>
//     *
//     * @param prompt The prompt definition including name and description
//     * @param promptHandler The function that processes prompt requests and returns
//     * formatted templates. The function's first argument is an
//     * {@link McpSyncServerExchange} upon which the server can interact with the connected
//     * client. The second arguments is a
//     * {@link io.modelcontextprotocol.spec.McpSchema.GetPromptRequest}.
//     */
//    public record SyncPromptSpecification(McpSchema.Prompt prompt,
//                                          BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler) {
//    }
//
//    /**
//     * Specification of a completion handler function with synchronous execution support.
//     *
//     * @param referenceKey The unique key representing the completion reference.
//     * @param completionHandler The synchronous function that processes completion
//     * requests and returns results. The first argument is an
//     * {@link McpSyncServerExchange} used to interact with the client. The second argument
//     * is a {@link io.modelcontextprotocol.spec.McpSchema.CompleteRequest}.
//     */
//    public record SyncCompletionSpecification(McpSchema.CompleteReference referenceKey,
//                                              BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> completionHandler) {
//    }
//
//
//}
