package com.breno.intellibuy.model.ai;

import com.breno.intellibuy.model.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_embeddings")
public class ProductEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id", unique = true)
    private Product product;

    @Column(name = "embedding", columnDefinition = "VECTOR(768)")
    private String embedding;

    @Column(columnDefinition = "TEXT")
    private String content;

    public ProductEmbedding(Product product, String embedding, String content) {
        this.product = product;
        this.embedding = embedding;
        this.content = content;
    }

}
