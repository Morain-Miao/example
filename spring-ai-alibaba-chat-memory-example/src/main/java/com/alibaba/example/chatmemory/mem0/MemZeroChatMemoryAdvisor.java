package com.alibaba.example.chatmemory.mem0;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.vectorstore.SearchRequest;
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
    public static final String TOP_K = "chat_memory_vector_store_top_k";

    private static final String DOCUMENT_METADATA_CONVERSATION_ID = "conversationId";

    private static final String DOCUMENT_METADATA_MESSAGE_TYPE = "messageType";

    public static final String RETRIEVED_DOCUMENTS = "mem0_retrieved_documents";

    private static final int DEFAULT_TOP_K = 20;

    private static final PromptTemplate DEFAULT_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate("""
			{instructions}

			Use the long term conversation memory from the LONG_TERM_MEMORY section to provide accurate answers.

			---------------------
			LONG_TERM_MEMORY:
			{long_term_memory}
			---------------------
			""");

    private final PromptTemplate systemPromptTemplate;

    private final int defaultTopK;

    private final String defaultConversationId;

    private final int order;

    private final Scheduler scheduler;

    private final MemZeroMemoryStore memZeroMemoryStore;

    public MemZeroChatMemoryAdvisor(PromptTemplate systemPromptTemplate, int defaultTopK, String defaultConversationId, int order, Scheduler scheduler, MemZeroMemoryStore memZeroMemoryStore) {
        this.systemPromptTemplate = systemPromptTemplate;
        this.defaultTopK = defaultTopK;
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
        SearchRequest searchRequest = MemZeroSearchRequest.builder()
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

    private Document toDocument(Hit<Document> hit) {
        Document document = hit.source();
        Document.Builder documentBuilder = document.mutate();
        if (hit.score() != null) {
            documentBuilder.metadata(DocumentMetadata.DISTANCE.value(), 1 - hit.score().floatValue());
            documentBuilder.score(hit.score());
        }
        return documentBuilder.build();
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
