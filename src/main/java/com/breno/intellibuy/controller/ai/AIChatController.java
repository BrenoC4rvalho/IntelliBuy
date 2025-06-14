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
    public ResponseEntity<?> askAboutProducts(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("A pergunta não pode ser vazia.");
        }
        String answer = productEmbeddingService.generateAnswer(query);
        return ResponseEntity.ok(answer);
    }

}
