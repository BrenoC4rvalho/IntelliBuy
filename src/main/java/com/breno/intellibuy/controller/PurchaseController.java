package com.breno.intellibuy.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {


    @Autowired
    private CompraService compraService;

    // Endpoint para criar uma nova compra (POST /compras)
    @PostMapping
    public ResponseEntity<Compra> createCompra(@RequestBody Compra compra) {
        try {
            Compra savedCompra = compraService.saveCompra(compra);
            return new ResponseEntity<>(savedCompra, HttpStatus.CREATED);
        } catch (Exception e) {
            // Em caso de erro (cliente/produto não encontrado, etc.),
            // a exceção ResponseStatusException já é tratada pelo Spring,
            // mas podemos adicionar um log ou tratamento mais específico aqui.
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint para buscar todas as compras (GET /compras)
    @GetMapping
    public ResponseEntity<List<Compra>> getAllCompras() {
        List<Compra> compras = compraService.getAllCompras();
        return new ResponseEntity<>(compras, HttpStatus.OK);
    }

    // Endpoint para buscar compra por ID (GET /compras/{id})
    @GetMapping("/{id}")
    public ResponseEntity<Compra> getCompraById(@PathVariable Long id) {
        return compraService.getCompraById(id)
                .map(compra -> new ResponseEntity<>(compra, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Endpoint para atualizar uma compra (PUT /compras/{id})
    @PutMapping("/{id}")
    public ResponseEntity<Compra> updateCompra(@PathVariable Long id, @RequestBody Compra compra) {
        try {
            Compra updatedCompra = compraService.updateCompra(id, compra);
            return new ResponseEntity<>(updatedCompra, HttpStatus.OK);
        } catch (Exception e) {
            // O tratamento de exceções pode ser mais elaborado,
            // com classes de erro customizadas e @ControllerAdvice
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint para deletar uma compra (DELETE /compras/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompra(@PathVariable Long id) {
        return compraService.getCompraById(id)
                .map(compra -> {
                    compraService.deleteCompra(id);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
