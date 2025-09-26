package com.portfolio.model;

import java.math.BigDecimal;

/**
 * Represents a position in the portfolio.
 * Contains the security and the number of shares/contracts held.
 */
public class Position {
    
    private String symbol;
    private BigDecimal positionSize;
    private Security security;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    
    // Constructors
    public Position() {}
    
    public Position(String symbol, BigDecimal positionSize) {
        this.symbol = symbol;
        this.positionSize = positionSize;
    }
    
    public Position(String symbol, BigDecimal positionSize, Security security) {
        this.symbol = symbol;
        this.positionSize = positionSize;
        this.security = security;
    }
    
    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public BigDecimal getPositionSize() {
        return positionSize;
    }
    
    public void setPositionSize(BigDecimal positionSize) {
        this.positionSize = positionSize;
    }
    
    public Security getSecurity() {
        return security;
    }
    
    public void setSecurity(Security security) {
        this.security = security;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public BigDecimal getMarketValue() {
        return marketValue;
    }
    
    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }
    
    /**
     * Calculates the market value of this position.
     * For stocks: Market Value = Position Size × Stock Price
     * For options: Market Value = Position Size × Option Price
     * Short positions are multiplied by -1
     */
    public BigDecimal calculateMarketValue() {
        if (currentPrice == null || positionSize == null) {
            return BigDecimal.ZERO;
        }
        return positionSize.multiply(currentPrice);
    }
    
    /**
     * Checks if this is a long position (positive position size)
     */
    public boolean isLongPosition() {
        return positionSize != null && positionSize.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if this is a short position (negative position size)
     */
    public boolean isShortPosition() {
        return positionSize != null && positionSize.compareTo(BigDecimal.ZERO) < 0;
    }
    
    @Override
    public String toString() {
        return "Position{" +
                "symbol='" + symbol + '\'' +
                ", positionSize=" + positionSize +
                ", currentPrice=" + currentPrice +
                ", marketValue=" + marketValue +
                '}';
    }
}
