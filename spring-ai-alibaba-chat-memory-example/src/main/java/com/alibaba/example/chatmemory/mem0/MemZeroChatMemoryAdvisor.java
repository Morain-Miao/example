package com.alibaba.example.chatmemory.mem0;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Memory is retrieved from a Mem0 added into the prompt's system text.
 * user text.
 * @author Morain Miao
 * @since 1.0.0
 */
public class MemZeroChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "mem0_retrieved_documents";

    private final PromptTemplate systemPromptTemplate;

    private final String defaultConversationId;

    private final int order;

    private final Scheduler scheduler;

    private final MemZeroMemoryStore memZeroMemoryStore;

    public MemZeroChatMemoryAdvisor(PromptTemplate systemPromptTemplate, String defaultConversationId, int order, Scheduler scheduler, MemZeroMemoryStore memZeroMemoryStore) {
        this.systemPromptTemplate = systemPromptTemplate;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
        this.memZeroMemoryStore = memZeroMemoryStore;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        // 1. Search for similar documents in the vector store.
        String conversationId = getConversationId(request.context(), this.defaultConversationId);
        String query = request.prompt().getUserMessage() != null ? request.prompt().getUserMessage().getText() : "";
        SearchRequest searchRequest = MemZeroRequest.SearchRequest.builder()
                .query(query)
                .build();

        List<Document> documents = this.memZeroMemoryStore.similaritySearch(searchRequest);

        // 2. Create the context from the documents.
        Map<String, Object> context = new HashMap<>(request.context());
        context.put(RETRIEVED_DOCUMENTS, documents);

        String documentContext = documents == null ? ""
                : documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        // 3. Augment the user prompt with the document context.
        UserMessage userMessage = request.prompt().getUserMessage();
        String augmentedUserText = this.systemPromptTemplate
                .render(Map.of("query", userMessage.getText(), "mem0_context", documentContext));

        if (StringUtils.hasText(userMessage.getText())) {
            this.memZeroMemoryStore.write(toDocuments(java.util.List.of(userMessage), conversationId));
        }
        // 4. Update ChatClientRequest with augmented prompt.
        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
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
        this.memZeroMemoryStore.write(toDocuments(assistantMessages,
                this.getConversationId(chatClientResponse.context(), this.defaultConversationId)));
        return chatClientResponse;
    }

    private List<Document> toDocuments(List<Message> messages, String conversationId) {
        List<Document> docs = messages.stream().filter((m) -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT).map((message) -> {
            HashMap<String, Object> metadata = new HashMap((Map)(message.getMetadata() != null ? message.getMetadata() : new HashMap()));
            metadata.put("conversationId", conversationId);
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
        public static final String RETRIEVED_DOCUMENTS = "mem0_retrieved_documents";

        private PromptTemplate systemPromptTemplate;
        private String defaultConversationId;
        private int order;
        private Scheduler scheduler;
        private MemZeroMemoryStore memZeroMemoryStore;

        protected Builder(VectorStore vectorStore) {
            this.defaultConversationId = "default";
            this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
            this.order = -2147482648;
            this.memZeroMemoryStore = (MemZeroMemoryStore) vectorStore;
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
            return new MemZeroChatMemoryAdvisor(this.systemPromptTemplate, this.defaultConversationId, this.order, this.scheduler, this.memZeroMemoryStore);
        }
    }
}
