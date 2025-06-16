package com.breno.intellibuy.config;

import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.services.ProductService;
import com.breno.intellibuy.services.ai.ProductEmbeddingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductEmbeddingService productEmbeddingService;
    private final ProductService productService;

    public DataLoader(ProductEmbeddingService productEmbeddingService, ProductService productService) {
        this.productEmbeddingService = productEmbeddingService;
        this.productService = productService;
    }

    @Override
    public void run(String... args) throws Exception {

        if (productService.getAll().isEmpty()) {
            System.out.println("No products found. Generating dummy product data...");
            generateDummyProducts(10);
        } else {
            System.out.println("Existing products detected. Skipping dummy data generation.");
        }

        productEmbeddingService.ingestAllProductsToVectorStore();
    }

    private void generateDummyProducts(int numberOfProducts) {

        Random random = new Random();

        String[] adjectives = {
                "Smart", "Portable", "Durable", "Modern", "Compact", "Efficient", "Fast", "Lightweight",
                "Innovative", "Premium", "Ergonomic", "Wireless", "Eco-Friendly", "Versatile", "Crystal Clear"
        };

        String[] nouns = {
                "Smartphone", "Laptop", "Headphones", "Smartwatch", "Camera", "Television", "Tablet",
                "Gaming Console", "Robot Vacuum", "Blender", "Coffee Maker", "Fitness Tracker",
                "Bluetooth Speaker", "Drone", "E-Reader"
        };

        String[] descriptions = {
                "Unleash incredible performance and long-lasting battery life for your daily tasks.",
                "Perfect for work and entertainment, featuring a stunning high-resolution display.",
                "Immersive audio experience with a comfortable, ergonomic design.",
                "Monitor your health and receive notifications right on your wrist.",
                "Capture unforgettable moments with professional-grade quality.",
                "Experience vibrant images and powerful sound, transforming your living room.",
                "Ideal for productivity and entertainment on the go, designed for ultimate portability.",
                "Engaging gaming experience with cutting-edge graphics and smooth gameplay.",
                "Autonomous and efficient cleaning solution for your smart home.",
                "Effortlessly blend your favorite smoothies and shakes with powerful blades.",
                "Enjoy barista-quality coffee at home with intuitive controls.",
                "Track your progress and stay motivated with advanced fitness metrics.",
                "Powerful sound in a compact design, perfect for any adventure.",
                "Explore the skies with easy-to-fly controls and a high-definition camera.",
                "Read comfortably for hours with a glare-free screen and adjustable light."
        };

        for (int i = 0; i < numberOfProducts; i++) {
            String name = adjectives[random.nextInt(adjectives.length)] + " " + nouns[random.nextInt(nouns.length)];
            String description = descriptions[random.nextInt(descriptions.length)];

            BigDecimal price = BigDecimal.valueOf(random.nextDouble() * (2000.00 - 50.00) + 50.00)
                    .setScale(2, RoundingMode.HALF_UP);


            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);

            productService.save(product);
            System.out.println("Dummy product created: " + product.getName() + " - $" + product.getPrice());
        }
        System.out.println(numberOfProducts + " dummy products successfully generated.");
    }

}
