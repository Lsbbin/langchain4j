package com.langchain.langchain4j.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private OllamaStreamingChatModel ollamaStreamingChatModel;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    private static final String DOCUMENT_PATH = "C:/Users/peaku/Desktop/sample/embedding"; // 문서 경로

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Assistant> assistantMap = new ConcurrentHashMap<>();
    private final List<Document> documents;

    public ChatWebSocketHandler() {
        // 문서 로드
        List<Document> loadedDocuments = loadDocumentsSafely(DOCUMENT_PATH);
        this.documents = loadedDocuments.stream()
                .flatMap(doc -> DocumentSplitters.recursive(512, 50).split(doc).stream()
                        .map(segment -> Document.from(segment.text(), doc.metadata())))
                .collect(Collectors.toList());
    }

    interface Assistant {
        @SystemMessage("""
            항상 친절하고 간결하게 답변하세요.
        """)
        TokenStream chat(String message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String sessionId = jsonNode.get("sessionId").asText();
            String userMessage = jsonNode.get("message").asText();

            TokenStream tokenStream = assistantMap
                    .computeIfAbsent(sessionId, k -> createAssistant()) // 세션별 Assistant 생성
                    .chat(userMessage);

            tokenStream.onPartialResponse(partialResponse -> {
                try {
                    session.sendMessage(new TextMessage(partialResponse));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).onCompleteResponse(response -> {
                try {
                    session.sendMessage(new TextMessage("[END]"));
                    session.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).onError(Throwable::printStackTrace).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Assistant 인스턴스 생성
    private Assistant createAssistant() {
        return AiServices.builder(Assistant.class)
                .streamingChatLanguageModel(ollamaStreamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContentRetriever())
                .build();
    }

    // ContentRetriever 생성
    private ContentRetriever createContentRetriever() {
        embeddingStoreIngestor.ingest(documents);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.8)
                .build();
    }

    // 문서를 안전하게 로드하는 메서드 (예외 발생 시 빈 리스트 반환)
    private static List<Document> loadDocumentsSafely(String path) {
        try {
            return FileSystemDocumentLoader.loadDocuments(path);
        } catch (Exception e) {
            System.err.println("Load Fail : " + e.getMessage());
            return Collections.emptyList();
        }
    }

}
