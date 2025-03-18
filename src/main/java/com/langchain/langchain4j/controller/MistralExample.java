package com.langchain.langchain4j.controller;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

import java.util.Scanner;

public class MistralExample {

    private static String MODEL_NAME = "llama3.2"; // Ollama에서 실행할 모델 이름
    private static String OLLAMA_URL = "http://localhost:11434"; // Ollama 기본 포트

    public static void main (String[] args) throws Exception {
        interface Assistant  {
            String chat(String message);
        }

        // Ollama를 사용하여 llama3.2 실행
        OllamaChatModel model = OllamaChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
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

}
