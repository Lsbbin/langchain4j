package com.langchain.langchain4j.service;

import com.langchain.langchain4j.mapper.ReviewMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    @Qualifier("reviewEmbeddingStore")
    private EmbeddingStore embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    private static final String userInput = "제품 리뷰를 종합 분석하여 요약하고 감정(긍정/부정) 및 주요 키워드를 추출해 주세요.";

    interface Assistant {
        @SystemMessage("""
            1. 당신은 제품 리뷰를 분석하여 요약하고, 감정 표현이 들어간 간결한 키워드를 추출하는 AI 입니다.
            2. 키워드는 다음 규칙을 따릅니다:
                - 3~6개 정도의 간결한 표현
                - 2~4글자 이내로 축약된 감정/의견 키워드
                - 반드시 대괄호 []로 묶어 출력
                - 문장형 표현이 아니라 형용사 또는 명사 위주의 간단한 말
                - 광고성, 응원성, 브랜드명, 제품명이 들어간 키워드는 절대 포함하지 마세요 (예: [홈플러스 번창] 등 제외)
            
            [답변 예시]
            요약 : 해당 제품은 배송이 빠르고 신선도가 높아 전반적으로 만족도가 높습니다. 다만 육질이 질기다는 의견도 있었습니다.
            감정 분석 : 긍정
            주요 키워드 : [배송 빠름], [신선함], [가성비 좋음]
        """)
        Result<String> answer(String message);
    }

    public Map getReviewContentRetriever() {
        Map resultMap = new HashMap();

        try {
            resultMap.put("responseCode", "200");

            Assistant assistant = createAssistant();

            resultMap.put("result", assistant.answer(userInput).content());
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("responseCode", "500");
        }

        return resultMap;
    }

    // Assistant 인스턴스 생성
    private Assistant createAssistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(ollamaChatModel)
                .contentRetriever(createContentRetriever())
                .build();
    }

    // ContentRetriever 생성
    private ContentRetriever createContentRetriever() {
        List<Map> reviewList = reviewMapper.contentsList();

        // DB에서 가져온 데이터를 Document로 변환
        String reviewContent = IntStream.range(0, reviewList.size())
                .filter(i -> reviewList.get(i) != null)
                .mapToObj(i -> (i + 1) + ". " + reviewList.get(i).get("contents"))
                .collect(Collectors.joining("\n"));

        Metadata metadata = new Metadata();
        Document reviewDocument = Document.from(reviewContent, metadata);

        // 일정 크기로 분할하여 저장
        List<Document> reviewDocuments = DocumentSplitters.recursive(512, 50)
                .split(reviewDocument)
                .stream()
                .map(segment -> Document.from(segment.text(), metadata))
                .collect(Collectors.toList());

        EmbeddingStoreIngestor embeddingStoreIngestor =  EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        embeddingStoreIngestor.ingest(reviewDocuments);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.8)
                .build();
    }
}
