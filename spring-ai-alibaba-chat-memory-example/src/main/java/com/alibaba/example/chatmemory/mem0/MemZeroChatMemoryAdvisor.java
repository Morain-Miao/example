package com.alibaba.example.chatmemory.mem0;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory is retrieved from a Mem0 added into the prompt's system text.
 * user text.
 * @author Morain Miao
 * @since 1.0.0
 */
public class MemZeroChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "mem0_retrieved_documents";

    public static final String USER_ID = "user_id";
    public static final String AGENT_ID = "agent_id";
    public static final String RUN_ID = "run_id";
    public static final String FILTERS = "filters";

    private static final PromptTemplate DEFAULT_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate("""
            ---------------------
            USER_INPUT_MESSAGE:
            {query}
            ---------------------
            Use the long term conversation memory from the LONG_TERM_MEMORY section to provide accurate answers.
   
            LONG_TERM_MEMORY is a dictionary containing the search results, typically under a "results" type, and potentially "relations" type if graph store is enabled.
            Example:
            ```text
            [
            	 {
            	  "type": "results",
                  "id": "...", # memory id
                  "memory": "...", # memory text
                  "hash": "...",  # memory hash value
                  "metadata": "...", # user custom dict
                  "score": 0.3,   # relevance score: the higher the score, the more relevant.
                  "created_at": "...", # created time
                  "updated_at": null, # updated time
                  "user_id": "...",
                  "agent_id": "...",
                  "run_id": "..."
                },
            	{
            	  "type": "relations",
                  "source": "...", // e.g.: means originated from user_id = _xxx
                  "relationship": "...", // e.g.: loves means hobby
                  "destination": "..."
                }
            ]
            ```
   
            ---------------------
            LONG_TERM_MEMORY:
            {long_term_memory}
            ---------------------
   """);

    private final PromptTemplate systemPromptTemplate;

    private final int order;

    private final Scheduler scheduler;

    private final VectorStore vectorStore;

    public MemZeroChatMemoryAdvisor(PromptTemplate systemPromptTemplate, int order, Scheduler scheduler, VectorStore vectorStore) {
        this.systemPromptTemplate = systemPromptTemplate;
        this.order = order;
        this.scheduler = scheduler;
        this.vectorStore = vectorStore;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        // 1. Search for similar documents in the vector store.
        UserMessage userMessage = request.prompt().getUserMessage();
        String query = userMessage != null ? userMessage.getText() : "";

        Map<String, Object> params = request.context();
        SearchRequest searchRequest = MemZeroServerRequest.SearchRequest.builder()
                .query(query)
                .userId(params.containsKey(USER_ID)? params.get(USER_ID).toString() : null)
                .agentId(params.containsKey(AGENT_ID)? params.get(AGENT_ID).toString() : null)
                .runId(params.containsKey(RUN_ID)? params.get(RUN_ID).toString() : null)
                .filters(params.containsKey(FILTERS) && params.get(FILTERS) instanceof Map? (Map<String, Object>) params.get(FILTERS) : null)
                .build();

        List<Document> documents = this.vectorStore.similaritySearch(searchRequest);

        String documentContext = documents == null ? ""
                : documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        // 3. Augment the user prompt with the document context.
        String augmentedUserText = this.systemPromptTemplate
                .render(Map.of("query", query, "long_term_memory", documentContext));

        if (Objects.nonNull(userMessage) && StringUtils.hasText(query)) {
            this.vectorStore.add(toDocuments(java.util.List.of(userMessage)));
        }
        // 4. Update ChatClientRequest with augmented prompt.
        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(augmentedUserText))
                .context(params)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        List<Message> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse()
                    .getResults()
                    .stream()
                    .map(g -> (Message) g.getOutput())
                    .toList();
        }

        // write mem0 memory
        this.vectorStore.add(toDocuments(assistantMessages));
        return chatClientResponse;
    }

    private List<Document> toDocuments(List<Message> messages) {
        List<Document> docs = messages.stream().filter((m) -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT).map((message) -> {
            HashMap<String, Object> metadata = new HashMap<>(message.getMetadata() != null ? message.getMetadata() : new HashMap());
            metadata.put("messageType", message.getMessageType().name());
            if (message instanceof UserMessage userMessage) {
                return Document.builder().text(userMessage.getText()).metadata(metadata).build();
            } else if (message instanceof AssistantMessage assistantMessage) {
                return Document.builder().text(assistantMessage.getText()).metadata(metadata).build();
            } else {
                throw new RuntimeException("Unknown message type: " + message.getMessageType());
            }
        }).toList();
        return docs;
    }

    private MemZeroServerRequest.SearchRequest getConversationId(Map<String, Object> context) {
        MemZeroServerRequest.SearchRequest build = MemZeroServerRequest.SearchRequest.builder()
                .userId(context.getOrDefault(USER_ID, "").toString())
                .build();
        return build;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public static MemZeroChatMemoryAdvisor.Builder builder(VectorStore chatMemory) {
        return new MemZeroChatMemoryAdvisor.Builder(chatMemory);
    }

    public static class Builder {
        private PromptTemplate systemPromptTemplate = DEFAULT_SYSTEM_PROMPT_TEMPLATE;
        private String defaultConversationId;
        private int order;
        private Scheduler scheduler;
        private final VectorStore vectorStore;

        protected Builder(VectorStore vectorStore) {
            this.defaultConversationId = "default";
            this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
            this.order = -2147482648;
            this.vectorStore = vectorStore;
        }

        public MemZeroChatMemoryAdvisor.Builder systemPromptTemplate(PromptTemplate systemPromptTemplate) {
            this.systemPromptTemplate = systemPromptTemplate;
            return this;
        }

        public MemZeroChatMemoryAdvisor.Builder conversationId(String conversationId) {
            this.defaultConversationId = conversationId;
            return this;
        }

        public MemZeroChatMemoryAdvisor.Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public MemZeroChatMemoryAdvisor.Builder order(int order) {
            this.order = order;
            return this;
        }

        public MemZeroChatMemoryAdvisor build() {
            return new MemZeroChatMemoryAdvisor(this.systemPromptTemplate, this.order, this.scheduler, this.vectorStore);
        }
    }
}
