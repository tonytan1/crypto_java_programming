package com.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a portfolio containing multiple positions.
 * Calculates the Net Asset Value (NAV) as the sum of all position market values.
 */
public class Portfolio {
    
    private List<Position> positions;
    private BigDecimal totalNAV;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public Portfolio() {
        this.positions = new ArrayList<>();
        this.totalNAV = BigDecimal.ZERO;
    }
    
    public Portfolio(List<Position> positions) {
        this.positions = positions != null ? new ArrayList<>(positions) : new ArrayList<>();
        this.totalNAV = BigDecimal.ZERO;
        calculateNAV();
    }
    
    // Getters and Setters
    public List<Position> getPositions() {
        return positions;
    }
    
    public void setPositions(List<Position> positions) {
        this.positions = positions != null ? new ArrayList<>(positions) : new ArrayList<>();
        if (this.positions != null) {
            calculateNAV();
        }
    }
    
    public BigDecimal getTotalNAV() {
        return totalNAV;
    }
    
    public void setTotalNAV(BigDecimal totalNAV) {
        this.totalNAV = totalNAV;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Adds a position to the portfolio
     */
    public void addPosition(Position position) {
        if (position != null) {
            positions.add(position);
            calculateNAV();
        }
    }
    
    /**
     * Removes a position from the portfolio
     */
    public void removePosition(Position position) {
        if (position != null) {
            positions.remove(position);
            calculateNAV();
        }
    }
    
    /**
     * Calculates the Net Asset Value (NAV) as the sum of all position market values
     */
    public BigDecimal calculateNAV() {
        if (positions == null) {
            totalNAV = BigDecimal.ZERO;
        } else {
            totalNAV = positions.stream()
                    .map(Position::calculateMarketValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        lastUpdated = LocalDateTime.now();
        return totalNAV;
    }
    
    /**
     * Gets the number of positions in the portfolio
     */
    public int getPositionCount() {
        return positions != null ? positions.size() : 0;
    }
    
    /**
     * Checks if the portfolio is empty
     */
    public boolean isEmpty() {
        return positions == null || positions.isEmpty();
    }
    
    /**
     * Gets a position by symbol
     */
    public Position getPositionBySymbol(String symbol) {
        if (positions == null) {
            return null;
        }
        return positions.stream()
                .filter(p -> p != null && p.getSymbol() != null && p.getSymbol().equals(symbol))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public String toString() {
        return "Portfolio{" +
                "positionCount=" + getPositionCount() +
                ", totalNAV=" + totalNAV +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
