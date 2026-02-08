package com.shopsphere.api.exceptions;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productId) {
        super("Insufficient stock available for product ID: " + productId);
    }
}
