package com.breno.intellibuy.services;

import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.repository.ProductRepository;
import com.breno.intellibuy.services.ai.DataEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final DataEmbeddingService dataEmbeddingService;

    public ProductService(ProductRepository productRepository, DataEmbeddingService dataEmbeddingService) {
        this.productRepository = productRepository;
        this.dataEmbeddingService = dataEmbeddingService;
    }

    public Product save(Product product) {
        Product savedProduct = productRepository.save(product);
        dataEmbeddingService.embedProduct(savedProduct);
        return savedProduct;
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Optional<Product> getById(Long id) {
        return productRepository.findById(id);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

}
