package com.langchain.langchain4j.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String OLLAMA_URL = "http://localhost:11434"; // Ollama 기본 포트
    private static final String MODEL_NAME = "gemma3:1b"; // Ollama에서 실행할 모델 이름
    private static final String DOCUMENT_PATH = "C:/Users/peaku/Desktop/sample/embedding"; // 문서 경로

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Assistant> assistantMap = new ConcurrentHashMap<>();

    interface Assistant {

        @SystemMessage("""
            당신은 사용자의 질문에 대해 답변하는 AI 입니다.
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

            // sessionId 기반으로 채팅 히스토리 저장
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

    // Assistant 인스턴스를 생성하는 메서드
    private static Assistant createAssistant() {
        OllamaStreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .build();

        List<Document> documents = loadDocumentsSafely(DOCUMENT_PATH);

        return AiServices.builder(Assistant.class)
                .streamingChatLanguageModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContentRetriever(documents))
                .build();
    }

    // 문서 전처리 후 저장
    private static ContentRetriever createContentRetriever(List<Document> documents) {
        if (documents.isEmpty()) {
            return query -> Collections.emptyList();  // 문서가 없을 경우 빈 리스트 반환
        }

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
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
