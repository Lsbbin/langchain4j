package com.langchain.langchain4j.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class MainController {

    private static final String FILE_PATH = "C:/Users/peaku/Desktop/sample/embedding/test.txt";

    @RequestMapping(value = "/")
    public String main(Model model) throws IOException {
        Path path = Paths.get(FILE_PATH);
        model.addAttribute("fileStr", new String(Files.readAllBytes(path)));
        return "index";
    }
}
