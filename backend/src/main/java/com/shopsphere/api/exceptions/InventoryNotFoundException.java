package com.shopsphere.api.exceptions;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String productId) {

        super("Inventory not found for product ID: " + productId);
    }
}
