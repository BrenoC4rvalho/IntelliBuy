package com.breno.intellibuy.repository.ai;

import com.breno.intellibuy.model.ai.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {

    @Query(value = "SELECT pe.* FROM product_embeddings pe ORDER BY pe.embedding <=> ?1 LIMIT ?2", nativeQuery = true)
    List<ProductEmbedding> findNearestNeighbors(String queryEmbedding, int topK);

    Optional<ProductEmbedding> findByProductId(Long productId);

}
