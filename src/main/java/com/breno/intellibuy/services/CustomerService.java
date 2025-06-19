package com.breno.intellibuy.services;

import com.breno.intellibuy.model.Customer;
import com.breno.intellibuy.repository.CustomerRepository;
import com.breno.intellibuy.services.ai.DataEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final DataEmbeddingService dataEmbeddingService;

    public CustomerService(CustomerRepository customerRepository, DataEmbeddingService dataEmbeddingService) {
        this.customerRepository = customerRepository;
        this.dataEmbeddingService = dataEmbeddingService;
    }

    public Customer save(Customer customer) {
        Customer savedCustomer = customerRepository.save(customer);
        dataEmbeddingService.embedCustomer(savedCustomer);
        return savedCustomer;
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getById(Long id) {
        return customerRepository.findById(id);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

}
