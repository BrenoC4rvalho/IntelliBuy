package com.breno.intellibuy.services.ai;


import com.breno.intellibuy.model.Customer;
import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.repository.CustomerRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataEmbeddingService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemMessage;

    public DataEmbeddingService(
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public void ingestAllDataToVectorStore() {
        List<Product> allProducts = productRepository.findAll();
        System.out.println("Starting ingestion of " + allProducts.size() + " products to Vector Store.");

        List<Document> productDocuments = allProducts.stream().map(product -> {
            String content = String.format("Product: %s. Description: %s. Price: $%.2f.",
                    product.getName(), product.getDescription(), product.getPrice());

            Map<String, Object> metadata = Map.of(
                    "type", "product",
                    "product_id", product.getId(),
                    "product_name", product.getName(),
                    "product_price", product.getPrice().doubleValue()
            );
            return new Document(content, metadata);
        }).toList();

        List<Customer> allCustomers = customerRepository.findAll();
        System.out.println("Starting ingestion of " + allCustomers.size() + " customers to Vector Store.");

        List<Document> customerDocuments = allCustomers.stream().map(customer -> {
            String content = String.format("Customer: %s. CPF: %s. Phone: %s.",
                    customer.getName(), customer.getCpf(), customer.getPhone());

            Map<String, Object> metadata = Map.of(
                    "type", "customer",
                    "customer_id", customer.getId(),
                    "customer_name", customer.getName()
            );
            return new Document(content, metadata);
        }).toList();

        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(productDocuments);
        allDocuments.addAll(customerDocuments);


        if (!allDocuments.isEmpty()) {
            vectorStore.add(allDocuments);
            System.out.println("Ingestion of all documents to Vector Store completed successfully.");
        } else {
            System.out.println("No data to ingest to Vector Store.");
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
