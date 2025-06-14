package com.breno.intellibuy.services.ai;

import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.model.ai.ProductEmbedding;
import com.breno.intellibuy.repository.ProductRepository;
import com.breno.intellibuy.repository.ai.ProductEmbeddingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductEmbeddingService {

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @Value("${ollama.embedding.model}")
    private String ollamaEmbeddingModel;

    @Value("${ollama.generation.model}")
    private String ollamaGenerationModel;

    private final RestTemplate restTemplate;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public ProductEmbeddingService(ProductEmbeddingRepository productEmbeddingRepository, ProductRepository productRepository) {
        this.productEmbeddingRepository = productEmbeddingRepository;
        this.productRepository = productRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String generateEmbedding(String text) {
        String url = ollamaApiUrl + "/api/embeddings";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaEmbeddingModel);
        requestBody.put("prompt", text);

        try {
            String response = restTemplate.postForObject(url, requestBody, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("embedding");

            if (embeddingNode.isArray()) {
                List<Double> embeddingList = objectMapper.convertValue(embeddingNode,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
                return embeddingList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ", "[", "]"));
            } else {
                throw new RuntimeException("Ollama's response does not contain 'embedding' as an array.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding with Ollama for text: '" + text.substring(0, Math.min(text.length(), 100)) + "...' - " + e.getMessage(), e);
        }
    }

    @Transactional
    public ProductEmbedding saveOrUpdateProductEmbedding(Product product) {

        String contentToEmbed = String.format("Product: %s. Description: %s. Price: $%.2f.",
                product.getName(), product.getDescription(), product.getPrice());

        String embeddingString = generateEmbedding(contentToEmbed);

        ProductEmbedding productEmbedding = productEmbeddingRepository.findByProductId(product.getId())
                .orElse(new ProductEmbedding());

        productEmbedding.setProduct(product);
        productEmbedding.setContent(contentToEmbed);
        productEmbedding.setEmbedding(embeddingString);

        return productEmbeddingRepository.save(productEmbedding);
    }

    public String generateAnswer(String query) {
        String queryEmbedding = generateEmbedding(query);

        List<ProductEmbedding> relevantProductEmbeddings = productEmbeddingRepository.findNearestNeighbors(queryEmbedding, 5);

        if (relevantProductEmbeddings.isEmpty()) {
            return "I did not find relevant product information in my catalog to answer your question.";
        }

        StringBuilder context = new StringBuilder();
        context.append("You are an e-commerce assistant. Based on the following product information, answer the user's question. If the question cannot be answered with the information provided, please say that you do not have enough data. Do not add extraneous information or make up products. /n/n");
        for (int i = 0; i < relevantProductEmbeddings.size(); i++) {
            context.append("Product Information ").append(i + 1).append(": ").append(relevantProductEmbeddings.get(i).getContent()).append("\n");
        }
        context.append("\nUser question: ").append(query).append("\n");
        context.append("Response:");

        return callOllamaForGeneration(context.toString());
    }

    private String callOllamaForGeneration(String prompt) {
        String url = ollamaApiUrl + "/api/generate";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaGenerationModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        try {
            String response = restTemplate.postForObject(url, requestBody, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode responseNode = root.path("response");

            if (responseNode.isTextual()) {
                return responseNode.asText();
            } else {
                throw new RuntimeException("Ollama's response does not contain 'response' as text.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response with Ollama: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void ingestAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        System.out.println("Starting ingestion/update of embeddings for " + allProducts.size() + " product.");
        allProducts.forEach(product -> {
            try {
                saveOrUpdateProductEmbedding(product);
                System.out.println("Product Embedding '" + product.getName() + "' saved/updated.");
            } catch (Exception e) {
                System.err.println("Error processing embedding for product " + product.getName() + " (ID: " + product.getId() + "): " + e.getMessage());
            }
        });
        System.out.println("Embedding ingestion/update completed.");
    }

}
