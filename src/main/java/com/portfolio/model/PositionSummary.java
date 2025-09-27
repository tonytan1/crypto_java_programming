package com.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Record representing a position summary with market value and changes.
 * Uses Java 17 Records for immutable data transfer objects.
 */
public record PositionSummary(
    String symbol,
    BigDecimal currentPrice,
    BigDecimal marketValue,
    BigDecimal previousPrice,
    BigDecimal priceChange,
    BigDecimal percentageChange,
    LocalDateTime lastUpdated
) {
    
    /**
     * Creates a PositionSummary with calculated changes
     */
    public static PositionSummary of(String symbol, BigDecimal currentPrice, BigDecimal marketValue, 
                                   BigDecimal previousPrice, LocalDateTime lastUpdated) {
        BigDecimal priceChange = previousPrice != null ? 
            currentPrice.subtract(previousPrice) : BigDecimal.ZERO;
        
        BigDecimal percentageChange = (previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) != 0) ?
            priceChange.divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                     .multiply(new BigDecimal("100")) : BigDecimal.ZERO;
        
        return new PositionSummary(symbol, currentPrice, marketValue, previousPrice, 
                                 priceChange, percentageChange, lastUpdated);
    }
    
    /**
     * Returns a formatted string representation using Text Blocks
     */
    public String toFormattedString() {
        return """
            Position: %s
            Current Price: $%s
            Market Value: $%s
            Price Change: $%s (%s%%)
            Last Updated: %s
            """.formatted(
                symbol,
                currentPrice.setScale(2, java.math.RoundingMode.HALF_UP),
                marketValue.setScale(2, java.math.RoundingMode.HALF_UP),
                priceChange.setScale(2, java.math.RoundingMode.HALF_UP),
                percentageChange.setScale(2, java.math.RoundingMode.HALF_UP),
                lastUpdated
            );
    }
    
    /**
     * Returns true if the position has gained value
     */
    public boolean isGain() {
        return priceChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Returns true if the position has lost value
     */
    public boolean isLoss() {
        return priceChange.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Returns true if the position value is unchanged
     */
    public boolean isUnchanged() {
        return priceChange.compareTo(BigDecimal.ZERO) == 0;
    }
}
