package com.example.cartrecovery.controller;

import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestBody Product product) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(product));
    }

    @GetMapping("/{productId}")
    public Product getProduct(@PathVariable Long productId) {
        return productService.getProductById(productId);
    }

    @GetMapping
    public List<Product> getProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/by-name")
    public Product getProductByName(@RequestParam String name) {
        return productService.getProductByName(name);
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    @GetMapping("/available")
    public List<Product> getAvailableProducts() {
        return productService.getAvailableProducts();
    }

    @PutMapping("/{productId}")
    public Product updateProduct(
            @PathVariable Long productId,
            @RequestBody Product product) {
        return productService.updateProduct(productId, product);
    }

    @PatchMapping("/{productId}/stock")
    public Product updateStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        return productService.updateStock(productId, quantity);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
