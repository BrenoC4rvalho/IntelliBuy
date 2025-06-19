package com.breno.intellibuy.services.ai;


import com.breno.intellibuy.model.Customer;
import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.model.Purchase;
import com.breno.intellibuy.repository.CustomerRepository;
import com.breno.intellibuy.repository.ProductRepository;
import com.breno.intellibuy.repository.PurchaseRepository;
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

    private static final String INITIAL_INGESTION_FLAG = "initial_ingestion_complete_v1";

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemMessage;

    public DataEmbeddingService(
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            PurchaseRepository purchaseRepository,
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public void runInitialEmbeddingIfNeeded() {
        if (!initialIngestionHasBeenPerformed()) {
            System.out.println("First time startup detected. Performing initial bulk data ingestion...");
            ingestAllData();
            createIngestionFlag();
            System.out.println("Initial bulk data ingestion complete.");
        } else {
            System.out.println("Initial ingestion flag found. Skipping bulk data ingestion.");
        }
    }
    private void ingestAllData() {

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

        List<Purchase> allPurchases = purchaseRepository.findAll();
        System.out.println("Starting ingestion of " + allPurchases.size() + " purchases to Vector Store.");
        List<Document> purchaseDocuments = allPurchases.stream().map(purchase -> {
            String itemsSummary = purchase.getPurchaseItem().stream()
                    .map(item -> String.format("%d unit(s) of %s", item.getQuantity(), item.getProduct().getName()))
                    .collect(Collectors.joining(", "));

            String content = String.format("Purchase made by customer %s on %s. Items: [%s]. Total value: $%.2f.",
                    purchase.getCustomer().getName(),
                    purchase.getDatePurchase().toLocalDate().toString(),
                    itemsSummary,
                    purchase.getTotalValue());

            Map<String, Object> metadata = Map.of(
                    "type", "purchase",
                    "purchase_id", purchase.getId(),
                    "customer_id", purchase.getCustomer().getId(),
                    "total_value", purchase.getTotalValue().doubleValue(),
                    "purchase_date", purchase.getDatePurchase().toString()
            );
            return new Document(content, metadata);
        }).toList();

        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(productDocuments);
        allDocuments.addAll(customerDocuments);
        allDocuments.addAll(purchaseDocuments);


        if (!allDocuments.isEmpty()) {
            vectorStore.add(allDocuments);
            System.out.println("Ingestion of all documents to Vector Store completed successfully.");
        } else {
            System.out.println("No data to ingest to Vector Store.");
        }
    }

    private boolean initialIngestionHasBeenPerformed() {
        SearchRequest request = SearchRequest.builder()
                .query("checking for ingestion flag")
                .filterExpression("ingestion_flag == '" + INITIAL_INGESTION_FLAG + "'")
                .topK(1)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        return !results.isEmpty();
    }

    private void createIngestionFlag() {
        Document flagDocument = new Document(
                "System flag: Initial data ingestion has been completed.",
                Map.of("type", "system_flag", "ingestion_flag", INITIAL_INGESTION_FLAG)
        );
        vectorStore.add(List.of(flagDocument));
    }

    public void embedProduct(Product product) {
        vectorStore.add(List.of(createProductDocument(product)));
    }

    private Document createProductDocument(Product product) {
        String content = String.format("Product: %s. Description: %s. Price: $%.2f.",
                product.getName(), product.getDescription(), product.getPrice());
        Map<String, Object> metadata = Map.of(
                "type", "product", "product_id", product.getId().toString()
        );
        return new Document(content, metadata);
    }

    public void embedCustomer(Customer customer) {
        vectorStore.add(List.of(createCustomerDocument(customer)));
    }

    private Document createCustomerDocument(Customer customer) {
        String content = String.format("Customer: %s. CPF: %s. Phone: %s.",
                customer.getName(), customer.getCpf(), customer.getPhone());
        Map<String, Object> metadata = Map.of(
                "type", "customer", "customer_id", customer.getId().toString()
        );
        return new Document(content, metadata);
    }

    public void embedPurchase(Purchase purchase) {
        vectorStore.add(List.of(createPurchaseDocument(purchase)));
    }

    private Document createPurchaseDocument(Purchase purchase) {
        String itemsSummary = purchase.getPurchaseItem().stream()
                .map(item -> String.format("%d unit(s) of %s", item.getQuantity(), item.getProduct().getName()))
                .collect(Collectors.joining(", "));
        String content = String.format("Purchase made by customer %s on %s. Items: [%s]. Total value: $%.2f.",
                purchase.getCustomer().getName(), purchase.getDatePurchase().toLocalDate().toString(), itemsSummary, purchase.getTotalValue());
        Map<String, Object> metadata = Map.of(
                "type", "purchase", "purchase_id", purchase.getId().toString()
        );
        return new Document(content, metadata);
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
