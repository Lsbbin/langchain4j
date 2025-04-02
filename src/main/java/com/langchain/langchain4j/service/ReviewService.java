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

    private static final String userInput = "키워드가 다양했으면 좋겠어요.";

    interface Assistant {
        @SystemMessage("""
            1.당신은 제품 리뷰 분석과 요약을 수행하는 AI 입니다.
            2.제품 리뷰들을 분석하고 요약하세요.
            3.감정(긍정, 부정)을 분석하고 결과를 제공하세요.
            4.고객들이 언급한 공통된 주제를 강조하세요.
            [답변 예시]
            해당 제품은 "고객들은 맛에 만족하며, 전반적으로 포장이 꼼꼼해 고객 만족도가 높습니다. 하지만 질감이 질기다는 평가도 있었습니다."
            주요 키워드는 [저렴해요], [비싸요], [배송이 빨라요]
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
