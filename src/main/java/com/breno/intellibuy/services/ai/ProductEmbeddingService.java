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
                throw new RuntimeException("Resposta do Ollama não contém 'embedding' como um array.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar embedding com Ollama para o texto: '" + text.substring(0, Math.min(text.length(), 100)) + "...' - " + e.getMessage(), e);
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
            return "Não encontrei informações relevantes de produtos em meu catálogo para responder a sua pergunta.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Você é um assistente de e-commerce. Com base nas seguintes informações de produtos, responda à pergunta do usuário. Se a pergunta não puder ser respondida com as informações fornecidas, diga que você não tem dados suficientes. Não adicione informações externas ou invente produtos.\n\n");
        for (int i = 0; i < relevantProductEmbeddings.size(); i++) {
            context.append("Informação do Produto ").append(i + 1).append(": ").append(relevantProductEmbeddings.get(i).getContent()).append("\n");
        }
        context.append("\nPergunta do Usuário: ").append(query).append("\n");
        context.append("Resposta:");

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
                throw new RuntimeException("Resposta do Ollama não contém 'response' como texto.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar resposta com Ollama: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void ingestAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        System.out.println("Iniciando ingestão/atualização de embeddings para " + allProducts.size() + " produtos.");
        allProducts.forEach(product -> {
            try {
                saveOrUpdateProductEmbedding(product);
                System.out.println("Embedding para produto '" + product.getName() + "' salvo/atualizado.");
            } catch (Exception e) {
                System.err.println("Erro ao processar embedding para o produto " + product.getName() + " (ID: " + product.getId() + "): " + e.getMessage());
            }
        });
        System.out.println("Ingestão/atualização de embeddings concluída.");
    }

}
