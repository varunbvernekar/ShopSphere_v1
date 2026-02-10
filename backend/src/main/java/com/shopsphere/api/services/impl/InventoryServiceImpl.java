package com.shopsphere.api.services.impl;

import com.shopsphere.api.dto.requestDTO.StockUpdateRequestDTO;
import com.shopsphere.api.dto.responseDTO.InventoryResponseDTO;
import com.shopsphere.api.entity.Inventory;
import com.shopsphere.api.repositories.InventoryRepository;
import com.shopsphere.api.services.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shopsphere.api.repositories.ProductRepository;
import com.shopsphere.api.exceptions.InventoryNotFoundException;
import com.shopsphere.api.exceptions.InsufficientStockException;
import com.shopsphere.api.exceptions.ProductNotFoundException;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    public InventoryResponseDTO getInventory(String productId) {
        validateProductExists(productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        return mapToResponse(inventory);
    }

    @Override
    public java.util.List<InventoryResponseDTO> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponseDTO updateInventory(String productId, StockUpdateRequestDTO request) {
        validateProductExists(productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        if (request.getQuantity() != null) {
            if (request.getQuantity() < 0) {
                throw new IllegalArgumentException("Stock cannot be negative");
            }
            inventory.setQuantity(request.getQuantity());
        }
        if (request.getThreshold() != null) {
            inventory.setReorderThreshold(request.getThreshold());
        }

        return mapToResponse(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional
    public void reduceStock(String productId, Integer quantity) {
        validateProductExists(productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        int newQuantity = inventory.getQuantity() - quantity;
        if (newQuantity < 0) {
            throw new InsufficientStockException(productId);
        }
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void increaseStock(String productId, Integer quantity) {
        validateProductExists(productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void initializeInventory(String productId, Integer quantity, Integer threshold) {
        validateProductExists(productId);
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            return; // Already exists
        }
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .quantity(quantity != null ? quantity : 0)
                .reorderThreshold(threshold != null ? threshold : 10)
                .build();
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void deleteInventory(String productId) {
        validateProductExists(productId);
        inventoryRepository.deleteByProductId(productId);
    }

    private void validateProductExists(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
    }

    private InventoryResponseDTO mapToResponse(Inventory inventory) {
        String productName = productRepository.findById(inventory.getProductId())
                .map(com.shopsphere.api.entity.Product::getName)
                .orElse("Unknown Product");
        return InventoryResponseDTO.fromEntity(inventory, productName);
    }
}
