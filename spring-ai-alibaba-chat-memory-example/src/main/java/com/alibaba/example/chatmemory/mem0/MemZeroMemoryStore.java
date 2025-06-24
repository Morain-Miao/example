package com.alibaba.example.chatmemory.mem0;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * @author miaoyumeng
 * @date 2025/06/24 14:28
 * @description TODO
 */
public class MemZeroMemoryStore extends AbstractObservationVectorStore implements InitializingBean {
    public MemZeroMemoryStore(AbstractVectorStoreBuilder<?> builder) {
        super(builder);
    }



    @Override
    public void doAdd(List<Document> documents) {

    }

    @Override
    public void doDelete(List<String> idList) {

    }

    @Override
    public List<Document> doSimilaritySearch(SearchRequest request) {
        return List.of();
    }

    @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
