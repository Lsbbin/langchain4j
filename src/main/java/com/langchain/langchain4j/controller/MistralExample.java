package com.langchain.langchain4j.controller;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.Scanner;

public class MistralExample {

    private static String MODEL_NAME = "llama3.2"; // Ollama에서 실행할 모델 이름
    private static String OLLAMA_URL = "http://localhost:11434"; // Ollama 기본 포트

    public static void main (String[] args) throws Exception {
        interface Assistant  {
            TokenStream chat(String message);
        }

        // Ollama를 사용하여 llama3.2 실행
        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl(OLLAMA_URL)
                .modelName(MODEL_NAME)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .streamingChatLanguageModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 사용자 입력을 받기 위한 Scanner 객체 생성
        Scanner scanner = new Scanner(System.in);
        System.out.print("질문을 입력하세요 : ");

        while (true) {
            String question = scanner.nextLine();

            // "exit" 또는 "quit" 입력 시 프로그램 종료
            if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
                System.out.println("대화를 종료합니다.");
                break;
            }

            // 모델에 질문을 보내고 응답 받기
            TokenStream tokenStream = assistant.chat(question);
            tokenStream
                    //.onRetrieved((List<Content> contents) -> System.out.println(contents))
                    //.onToolExecuted((ToolExecution toolExecution) -> System.out.println(toolExecution))
                    .onPartialResponse((String partialResponse) -> {
                        System.out.print(partialResponse);
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        System.out.println();
                        System.out.print("> ");
                    })
                    .onError((Throwable error) -> error.printStackTrace())
                    .start();
        }

        scanner.close();
    }

}
