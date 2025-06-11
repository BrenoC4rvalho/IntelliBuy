package com.breno.intellibuy.services;

import com.breno.intellibuy.model.Customer;
import com.breno.intellibuy.model.Product;
import com.breno.intellibuy.model.Purchase;
import com.breno.intellibuy.model.PurchaseItem;
import com.breno.intellibuy.repository.CustomerRepository;
import com.breno.intellibuy.repository.ProductRepository;
import com.breno.intellibuy.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Purchase save(Purchase purchase) {
        Customer customer = customerRepository.findById(purchase.getCustomer().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found!"));
        purchase.setCustomer(customer);

        BigDecimal totalValue = new BigDecimal("0.0");

        if (purchase.getPurchaseItem() != null && !purchase.getPurchaseItem().isEmpty()) {
            for (PurchaseItem item : purchase.getPurchaseItem()) {
                Product product = productRepository.findById(item.getProduct().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + item.getProduct().getId()));

                item.setProduct(product);
                item.setPurchase(purchase);
                totalValue = totalValue.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A purchase only or more item.");
        }

        purchase.setDatePurchase(LocalDateTime.now());
        purchase.setTotalValue(totalValue);

        return purchaseRepository.save(purchase);
    }

    public List<Purchase> getAll() {
        return purchaseRepository.findAll();
    }

    public Optional<Purchase> getById(Long id) {
        return purchaseRepository.findById(id);
    }

    public void delete(Long id) {
        purchaseRepository.deleteById(id);
    }

    @Transactional
    public Purchase updatePurchase(Long id, Purchase updatePurchase) {
        return purchaseRepository.findById(id)
                .map(existsPurchase -> {
                    if (updatePurchase.getCustomer() != null && updatePurchase.getCustomer().getId() != null) {
                        Customer customer = customerRepository.findById(updatePurchase.getCustomer().getId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found!"));
                        existsPurchase.setCustomer(customer);
                    }

                    if (updatePurchase.getPurchaseItem() != null) {
                        existsPurchase.getPurchaseItem().clear();
                        BigDecimal newTotalValue = new BigDecimal("0.0");
                        for (PurchaseItem newItem : updatePurchase.getPurchaseItem()) {
                            Product product = productRepository.findById(newItem.getProduct().getId())
                                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + newItem.getProduct().getId()));
                            newItem.setProduct(product);
                            newItem.setPurchase(existsPurchase);
                            existsPurchase.getPurchaseItem().add(newItem);
                            newTotalValue = newTotalValue.add(product.getPrice().multiply(BigDecimal.valueOf(newItem.getQuantity())));
                        }
                        existsPurchase.setTotalValue(newTotalValue);
                    }

                    return purchaseRepository.save(existsPurchase);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found!"));
    }

}
