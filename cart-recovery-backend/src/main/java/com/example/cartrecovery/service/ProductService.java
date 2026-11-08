package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.Product;
import com.example.cartrecovery.exception.ProductNotFoundException;
import com.example.cartrecovery.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

 public Product createProduct(Product product) {
    if (productRepository.existsByProductName(product.getProductName())) {
        throw new IllegalArgumentException("Product name already exists:" + product.getProductName());
    }
    if (product.getCreatedAt() == null) {
        product.setCreatedAt(LocalDateTime.now());
    }

        return productRepository.save(product);


 }

 public Product getProductById(Long productId) {

      return productRepository.findById(productId)
              .orElseThrow(() -> new ProductNotFoundException(productId));
 }
    public Product getProductByName(String productName) {

        return productRepository.findByProductName(productName)
                .orElseThrow(() -> new ProductNotFoundException(productName));
    }
    public List<Product> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC,"productId"));
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository
                .findByProductNameContainingIgnoreCase(keyword);
    }

        public List<Product> getAvailableProducts() {
            return productRepository
                    .findByStockQuantityGreaterThan(0);
        }

    public Product updateProduct(Long productId, Product updatedProduct) {

        Product existingProduct = getProductById(productId);

        if (!existingProduct.getProductName()
                .equalsIgnoreCase(updatedProduct.getProductName())
                && productRepository.existsByProductName(
                updatedProduct.getProductName())) {

            throw new IllegalArgumentException(
                    "Another product already exists with name: "
                            + updatedProduct.getProductName()
            );
        }

        existingProduct.setProductName(updatedProduct.getProductName());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setStockQuantity(updatedProduct.getStockQuantity());
        existingProduct.setDescription(updatedProduct.getDescription());


        return productRepository.save(existingProduct);
    }

    /**
     * Update stock quantity
     */
    public Product updateStock(Long productId, Integer stockQuantity) {

        Product product = getProductById(productId);

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity must not be negative."
            );
        }

        product.setStockQuantity(stockQuantity);

        return productRepository.save(product);
    }

    /**
     * Delete product
     */
    public void deleteProduct(Long productId) {

        Product product = getProductById(productId);

        productRepository.delete(product);
    }

    }
