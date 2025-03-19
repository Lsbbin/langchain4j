package com.langchain.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public class ChatExample {

    private static final String MODEL_NAME = "gemma3:1b"; // Ollama에서 실행할 모델 이름
    private static final String OLLAMA_URL = "http://localhost:11434"; // Ollama 기본 포트
    private static final String DOCUMENT_PATH = "C:/Users/peaku/Desktop/sample/embedding"; // 문서 경로

    public static void main (String[] args) throws Exception {
        interface Assistant  {

            @SystemMessage("친절하게 한국어로만 답변해.")
            String chat(String message);
        }

        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .timeout(Duration.ofSeconds(60))
                .build();

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(DOCUMENT_PATH);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContentRetriever(documents))
                .build();

        // 사용자 입력을 받기 위한 Scanner 객체 생성
        Scanner scanner = new Scanner(System.in);
        System.out.print("질문을 입력하세요 : ");

        while (true) {
            System.out.print("> ");
            String question = scanner.nextLine();

            // "exit" 또는 "quit" 입력 시 프로그램 종료
            if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
                System.out.println("대화를 종료합니다.");
                break;
            }

            // 모델에 질문을 보내고 응답 받기
            System.out.println(assistant.chat(question));
        }

        scanner.close();
    }

    // 문서 전처리 후 저장
    private static ContentRetriever createContentRetriever(List<Document> documents) {

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }

}
