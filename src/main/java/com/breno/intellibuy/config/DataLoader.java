package com.breno.intellibuy.config;

import com.breno.intellibuy.model.Customer;
import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.model.Purchase;
import com.breno.intellibuy.model.PurchaseItem;
import com.breno.intellibuy.services.CustomerService;
import com.breno.intellibuy.services.ProductService;
import com.breno.intellibuy.services.PurchaseService;
import com.breno.intellibuy.services.ai.DataEmbeddingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class DataLoader implements CommandLineRunner {

    private final DataEmbeddingService dataEmbeddingService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final PurchaseService purchaseService;

    public DataLoader(
            DataEmbeddingService dataEmbeddingService,
            ProductService productService,
            CustomerService customerService,
            PurchaseService purchaseService
    ) {
        this.dataEmbeddingService = dataEmbeddingService;
        this.productService = productService;
        this.customerService = customerService;
        this.purchaseService = purchaseService;
    }

    @Override
    public void run(String... args) throws Exception {

        if (productService.getAll().isEmpty()) {
            System.out.println("No products found. Generating dummy product data...");
            generateDummyProducts(10);
        } else {
            System.out.println("Existing products detected. Skipping dummy data generation.");
        }

        if (customerService.getAll().isEmpty()) {
            System.out.println("No customers found. Generating dummy customer data...");
            generateDummyCustomers();
        } else {
            System.out.println("Existing customers detected. Skipping dummy data generation.");
        }

        if (purchaseService.getAll().isEmpty()) {
            System.out.println("No purchases found. Generating dummy purchase data...");
            generateDummyPurchases();
        } else {
            System.out.println("Existing purchases detected. Skipping dummy data generation.");
        }

        dataEmbeddingService.runInitialEmbeddingIfNeeded();

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

    private void generateDummyCustomers() {

        String[] names = { "Mia", "Bryan", "Robert", "John", "Maya", "Steve", "Mohamed", "Emma", "Sophia", "Harper" };

        String[] listCPF = { "12345", "12346", "12347", "12348", "12349", "12351", "12352", "12353", "12354", "12355" };

        String[] listPhone = { "9911", "9922", "9933", "9944", "9955", "9966", "9977", "9988", "9999", "9900" };

        for (int i = 0; i < 10; i++) {

            Customer customer = new Customer();
            customer.setName(names[i]);
            customer.setCpf(listCPF[i]);
            customer.setPhone(listPhone[i]);

            customerService.save(customer);
            System.out.println("Dummy customer created: " + customer.getName());
        }

        System.out.println("Dummy customers successfully generated");

    }

    private void generateDummyPurchases() {

        Random random = new Random();
        List<Customer> customers = customerService.getAll();
        List<Product> products = productService.getAll();

        if (customers.isEmpty() || products.isEmpty()) {
            System.out.println("Cannot generate purchases without customers and products.");
            return;
        }

        for (int i = 0; i < 10; i++) {
            Purchase purchase = new Purchase();

            Customer randomCustomer = customers.get(random.nextInt(customers.size()));
            purchase.setCustomer(randomCustomer);

            List<PurchaseItem> items = new ArrayList<>();
            Collections.shuffle(products);

            int numberOfItems = random.nextInt(2) + 1;
            for (int j = 0; j < numberOfItems; j++) {
                Product randomProduct = products.get(j);
                int quantity = random.nextInt(2) + 1;

                PurchaseItem item = new PurchaseItem();
                item.setProduct(randomProduct);
                item.setQuantity(quantity);
                items.add(item);
            }

            purchase.setPurchaseItem(items);

            try {
                purchaseService.save(purchase);
                System.out.println("Dummy purchase created for customer: " + randomCustomer.getName());
            } catch (Exception e) {
                System.err.println("Error creating dummy purchase: " + e.getMessage());
            }
        }
        System.out.println("Dummy purchases successfully generated.");
    }

}
