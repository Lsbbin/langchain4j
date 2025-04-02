package com.langchain.langchain4j.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class OllamaConfig {

    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL_NAME = "gemma3:4b";
    private static final String EMBEDDING_MODEL_NAME = "granite-embedding:278m";

    @Bean
    public OllamaStreamingChatModel ollamaStreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .build();
    }

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .build();
    }

    @Bean(name = "reviewEmbeddingStore")
    public EmbeddingStore<TextSegment> reviewEmbeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000")
                .collectionName("review_" + UUID.randomUUID())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean(name = "chatEmbeddingStore")
    public EmbeddingStore<TextSegment> chatEmbeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000")
                .collectionName("chat_" + UUID.randomUUID())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(EMBEDDING_MODEL_NAME)
                .build();
    }

}
