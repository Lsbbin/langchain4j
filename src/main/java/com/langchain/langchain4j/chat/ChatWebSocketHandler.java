package com.langchain.langchain4j.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langchain.langchain4j.service.MemberService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private MemberService memberService;

    private static final String OLLAMA_URL = "http://localhost:11434"; // Ollama 기본 포트
    private static final String MODEL_NAME = "gemma3:4b"; // Ollama에서 실행할 모델 이름
    private static final String DOCUMENT_PATH = "C:/Users/peaku/Desktop/sample/embedding"; // 문서 경로

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Assistant> assistantMap = new ConcurrentHashMap<>();
    private final List<Document> documents;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public ChatWebSocketHandler() {
        // 문서 로드 및 벡터 스토어 초기화
        this.documents = loadDocumentsSafely(DOCUMENT_PATH);
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
    }

    interface Assistant {
        @SystemMessage("""
            1.당신은 "화려한 덕후들" 기업의 인사 담당자 입니다.
            2.항상 친절하게 답변하세요.
            3.먼저 물어보기 전에는 회사에 대해 소개하지마세요.
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

    // Assistant 인스턴스를 생성하는 메서드
    private Assistant createAssistant() {
        OllamaStreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .build();

        return AiServices.builder(Assistant.class)
                .streamingChatLanguageModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContentRetriever())
                .build();
    }

    // 문서와 DB에서 가져온 회원 정보를 결합하여 ContentRetriever 생성
    private ContentRetriever createContentRetriever() {
        List<Map> memberList = memberService.list();

        // DB에서 가져온 데이터를 Document로 변환
        String membersContent = "[사원 리스트]\n\n" + memberList.stream()
                .map(mem -> "이름: " + mem.get("NAME") + ", 이메일: " + mem.get("EMAIL") + ", 주소: " + mem.get("ADDRESS") + ", 직위: " + mem.get("POSITION"))
                .collect(Collectors.joining("\n"));

        Metadata metadata = new Metadata();
        Document memberDocument = Document.from(membersContent, metadata);

        EmbeddingStoreIngestor.ingest(memberDocument, embeddingStore);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
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
