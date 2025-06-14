package com.breno.intellibuy.config;

import com.breno.intellibuy.services.ai.ProductEmbeddingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductEmbeddingService productEmbeddingService;

    public DataLoader(ProductEmbeddingService productEmbeddingService) {
        this.productEmbeddingService = productEmbeddingService;
    }

    @Override
    public void run(String... args) throws Exception {
        productEmbeddingService.ingestAllProducts();
    }

}
