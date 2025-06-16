package com.breno.intellibuy.services.ai;


import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.repository.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductEmbeddingService {

    private final ProductRepository productRepository;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemMessage;

    public ProductEmbeddingService(ProductRepository productRepository, VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.productRepository = productRepository;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public void ingestAllProductsToVectorStore() {
        List<Product> allProducts = productRepository.findAll();
        System.out.println("Starting ingestion of " + allProducts.size() + " products to Vector Store.");

        List<Document> documents = allProducts.stream().map(product -> {
            String content = String.format("Product: %s. Description: %s. Price: $%.2f.",
                    product.getName(), product.getDescription(), product.getPrice());

            Map<String, Object> metadata = Map.of(
                    "product_id", product.getId(),
                    "product_name", product.getName(),
                    "product_price", product.getPrice().doubleValue()
            );
            return new Document(content, metadata);
        }).collect(Collectors.toList());

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            System.out.println("Ingestion of product embeddings to Vector Store completed successfully.");
        } else {
            System.out.println("No products to ingest to Vector Store.");
        }
    }

    public String generateAnswer(String query) {
        List<Document> relevantDocuments = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .build()
        );

        if (relevantDocuments.isEmpty()) {
            return "No relevant product information found in my catalog to answer your question.";
        }

        String context = relevantDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemMessage);
        Prompt prompt = new Prompt(systemPromptTemplate.createMessage(Map.of("context", context, "query", query)));

        return chatClient.prompt(prompt).call().content();
    }

}
