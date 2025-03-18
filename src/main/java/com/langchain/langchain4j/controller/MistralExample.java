package com.langchain.langchain4j.controller;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.util.Scanner;

@Controller
public class MistralExample {

    public static void main(String[] args) throws Exception {
        // Ollama를 사용하여 llama3.2 실행
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("llama3.2")  // Ollama에서 실행할 모델 이름
                .baseUrl("http://localhost:11434") // Ollama 기본 포트
                .timeout(Duration.ofSeconds(120))
                .build();

        // 사용자 입력을 받기 위한 Scanner 객체 생성
        Scanner scanner = new Scanner(System.in);
        System.out.println("질문을 입력하세요:");

        while (true) {
            System.out.print("> ");
            String question = scanner.nextLine();

            // "exit" 또는 "quit" 입력 시 프로그램 종료
            if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
                System.out.println("대화를 종료합니다.");
                break;
            }

            // 모델에 질문을 보내고 응답 받기
            String answer = model.generate(question);
            System.out.println("Llama 3: " + answer);
        }

        scanner.close();
    }

}
