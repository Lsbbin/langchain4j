package com.langchain.langchain4j.controller;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.stereotype.Controller;

@Controller
public class MistralExample {

    public static void main(String[] args) {
        // Ollama를 사용하여 Mistral 7B 실행
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("mistral")  // Ollama에서 실행할 모델 이름
                .baseUrl("http://localhost:11434") // Ollama 기본 포트
                .build();

        // 모델에 질문 보내기
        String response = model.generate("Langchain4j에 대해 설명 좀 해줘?");
        System.out.println("🤖 Mistral 7B Response: " + response);
    }

}
