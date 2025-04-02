package com.langchain.langchain4j.controller;

import com.langchain.langchain4j.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewApiController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/summarize")
    public ResponseEntity<Map> summarizeReviews() {
        return ResponseEntity.ok(reviewService.getReviewContentRetriever());
    }

}
