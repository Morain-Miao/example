package com.alibaba.example.chatmemory.mem0;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author miaoyumeng
 * @date 2025/06/24 14:28
 * @description TODO
 */
public class MemZeroMemoryStore  implements InitializingBean, VectorStore {

    private final MemZeroServiceClient mem0Client;
    private final ObjectMapper objectMapper;
    private final MemZeroFilterExpressionConverter mem0FilterExpressionConverter;

    protected MemZeroMemoryStore(MemZeroServiceClient client) {
        this.mem0Client = client;
        this.mem0FilterExpressionConverter = new MemZeroFilterExpressionConverter();
        this.objectMapper = JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules()).build();
    }

    public static MemZeroMemoryStoreBuilder builder(MemZeroServiceClient client) {
        return new MemZeroMemoryStoreBuilder(client);
    }

    public static final class MemZeroMemoryStoreBuilder{
        private final MemZeroServiceClient client;

        public MemZeroMemoryStoreBuilder(MemZeroServiceClient client) {
            this.client = client;
        }

        public MemZeroMemoryStore build() {
            return new MemZeroMemoryStore(client);
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void add(List<Document> documents) {
        //TODO 将role相同的message合并
        List<MemZeroServerRequest.MemoryCreate> messages = documents.stream().map(doc -> {
            MemZeroServerRequest.MemoryCreate create = MemZeroServerRequest.MemoryCreate.builder()
                    .messages(List.of(new MemZeroServerRequest.Message(
                            doc.getMetadata().get("role").toString(),
                            doc.getText()
                    )))
                    .metadata(doc.getMetadata())
                    .build();
            if (doc.getMetadata().containsKey("agentId")){
                create.setAgentId(doc.getMetadata().get("agentId").toString());
            }
            if (doc.getMetadata().containsKey("runId")){
                create.setRunId(doc.getMetadata().get("runId").toString());
            }
            if (doc.getMetadata().containsKey("userId")){
                create.setUserId(doc.getMetadata().get("userId").toString());
            }
            return create;
        }).toList();
        //TODO 增加异步方式
        messages.forEach(mem0Client::addMemory);
    }

    @Override
    public void delete(List<String> idList) {
        idList.forEach(mem0Client::deleteMemory);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException("The Mem0 Server only supports delete operation that must include userId, agentId, or runId.");
    }

    @Override
    public List<Document> similaritySearch(String query) {
        throw new UnsupportedOperationException("The Mem0 Server only supports queries that must include userId, agentId, or runId.");
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        MemZeroServerRequest.SearchRequest search = (MemZeroServerRequest.SearchRequest) request;

        if (request.getFilterExpression() != null){
            String jsonStr = this.mem0FilterExpressionConverter.convertExpression(request.getFilterExpression());

            Map<String, Object> filtersMap = null;
            if (jsonStr != null && !jsonStr.isEmpty()) {
                try {
                    filtersMap = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    // 如果转换失败，使用空Map
                    filtersMap = new HashMap<>();
                }
                search.setFilters(filtersMap);
            }
        }

        MemZeroServerResp memZeroServerResp = mem0Client.searchMemories(search);
        List<MemZeroServerResp.MemZeroResults> results = memZeroServerResp.getResults();
        List<MemZeroServerResp.MemZeroRelation> relations = memZeroServerResp.getRelations();

        List<Document> documents = Stream.concat(
                results.stream().map(r -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("type", "results");
                    meta.put("id", r.getId());
                    meta.put("memory", r.getMemory());
                    meta.put("hash", r.getHash());
                    meta.put("created_at", r.getCreatedAt());
                    meta.put("updated_at", r.getUpdatedAt());
                    meta.put("user_id", r.getUserId());
                    meta.put("agent_id", r.getAgentId());
                    meta.put("run_id", r.getRunId());
                    meta.put("score", r.getScore());
                    meta.put("metadata", r.getMetadata());

                    if (r.getMetadata() != null) meta.putAll(r.getMetadata());
                    return new Document(r.getId(), r.getMemory(), meta);
                }),
                relations.stream().map(rel -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("type", "relations");
                    meta.put("source", rel.getSource());
                    meta.put("relationship", rel.getRelationship());
                    meta.put("target", rel.getTarget());
                    String text = rel.getSource() + " --[" + rel.getRelationship() + "]--> " + rel.getTarget();
                    return new Document(text, meta);
                })
        ).toList();
        return documents;

    }
}
