package com.langchain.langchain4j.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileWriter;
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

    @PostMapping(value = "/saveFile")
    public ResponseEntity saveFile(@RequestParam("content") String content) {
        try {
            File file = new File(FILE_PATH);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            return new ResponseEntity(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
