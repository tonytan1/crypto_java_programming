package com.portfolio.model;

/**
 * Enumeration representing the types of securities supported by the system.
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
    
    @Override
    public String toString() {
        return description;
    }
}
