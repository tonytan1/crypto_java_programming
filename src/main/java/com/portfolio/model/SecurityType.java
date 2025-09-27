package com.portfolio.model;

/**
 * Enumeration representing the types of securities supported by the system.
 * Modernized with Java 17 features.
 */
public enum SecurityType {
    STOCK("Stock"),
    CALL("Call Option"),
    PUT("Put Option");
    
    private final String description;
    
    SecurityType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the risk weight for each security type using Switch Expressions
     */
    public double getRiskWeight() {
        return switch (this) {
            case STOCK -> 1.0;
            case CALL -> 1.2;
            case PUT -> 0.8;
        };
    }
    
    /**
     * Returns true if this is an option type using Switch Expressions
     */
    public boolean isOption() {
        return switch (this) {
            case CALL, PUT -> true;
            case STOCK -> false;
        };
    }
    
    /**
     * Returns the pricing model for each security type using Switch Expressions
     */
    public String getPricingModel() {
        return switch (this) {
            case STOCK -> "Current Market Price";
            case CALL, PUT -> "Black-Scholes Model";
        };
    }
    
    @Override
    public String toString() {
        return description;
    }
}
