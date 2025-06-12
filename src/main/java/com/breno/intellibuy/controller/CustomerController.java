package com.breno.intellibuy.controller;

import com.breno.intellibuy.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // Endpoint para criar um novo cliente (POST /clientes)
    @PostMapping
    public ResponseEntity<Cliente> createCliente(@RequestBody Cliente cliente) {
        Cliente savedCliente = clienteService.saveCliente(cliente);
        return new ResponseEntity<>(savedCliente, HttpStatus.CREATED); // Retorna 201 Created
    }

    // Endpoint para buscar todos os clientes (GET /clientes)
    @GetMapping
    public ResponseEntity<List<Cliente>> getAllClientes() {
        List<Cliente> clientes = clienteService.getAllClientes();
        return new ResponseEntity<>(clientes, HttpStatus.OK); // Retorna 200 OK
    }

    // Endpoint para buscar cliente por ID (GET /clientes/{id})
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> getClienteById(@PathVariable Long id) {
        return clienteService.getClienteById(id)
                .map(cliente -> new ResponseEntity<>(cliente, HttpStatus.OK)) // Retorna 200 OK se encontrado
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND)); // Retorna 404 Not Found se não encontrado
    }

    // Endpoint para atualizar um cliente (PUT /clientes/{id})
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> updateCliente(@PathVariable Long id, @RequestBody Cliente cliente) {
        return clienteService.getClienteById(id)
                .map(clienteExistente -> {
                    cliente.setId(id); // Garante que o ID do cliente seja o do path
                    Cliente updatedCliente = clienteService.saveCliente(cliente);
                    return new ResponseEntity<>(updatedCliente, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Endpoint para deletar um cliente (DELETE /clientes/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id) {
        return clienteService.getClienteById(id)
                .map(cliente -> {
                    clienteService.deleteCliente(id);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Retorna 204 No Content
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
