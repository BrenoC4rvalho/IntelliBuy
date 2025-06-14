package com.breno.intellibuy.controller.ai;

import com.breno.intellibuy.services.ai.ProductEmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/products")
public class AIChatController {

    private final ProductEmbeddingService productEmbeddingService;

    public AIChatController(ProductEmbeddingService productEmbeddingService) {
        this.productEmbeddingService = productEmbeddingService;
    }

    @GetMapping("/ask")
    public ResponseEntity<?> askAboutProducts(@RequestParam String question) {
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("The question cannot be empty.");
        }
        String answer = productEmbeddingService.generateAnswer(question);
        return ResponseEntity.ok(answer);
    }

}
