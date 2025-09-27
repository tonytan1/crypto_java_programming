package com.portfolio.service;

import com.portfolio.event.EventPublisher;
import com.portfolio.model.Position;
import com.portfolio.model.Portfolio;
import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for calculating portfolio values and NAV.
 * Handles both stock and option pricing calculations.
 * Thread-safe implementation using ReadWriteLock for optimal read performance.
 */
@Service
public class PortfolioCalculationService {
    
    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    private OptionPricingService optionPricingService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    // ReadWriteLock for thread-safe portfolio operations
    // Read-heavy workload: multiple reads, fewer writes
    private final ReadWriteLock portfolioLock = new ReentrantReadWriteLock();
    
    /**
     * Calculates market values for all positions in the portfolio
     * WRITE operation - requires exclusive access
     */
    public void calculatePortfolioValues(Portfolio portfolio) {
        long startTime = System.currentTimeMillis();
        
        portfolioLock.writeLock().lock();
        try {
            List<Position> positions = portfolio.getPositions();
            
            for (Position position : positions) {
                calculatePositionValue(position);
            }
            
            portfolio.calculateNAV();
            long calculationTime = System.currentTimeMillis() - startTime;
            eventPublisher.publishPortfolioRecalculated(
                portfolio.getTotalNAV(), 
                portfolio.getPositionCount(), 
                calculationTime
            );
            
        } finally {
            portfolioLock.writeLock().unlock();
        }
    }
    
    /**
     * Calculates the market value for a single position
     * WRITE operation - requires exclusive access
     */
    public void calculatePositionValue(Position position) {
        portfolioLock.writeLock().lock();
        try {
            Security security = position.getSecurity();
            if (security == null) {
                position.setCurrentPrice(BigDecimal.ZERO);
                position.setMarketValue(BigDecimal.ZERO);
                return;
            }
            
            BigDecimal currentPrice;
            
            if (security.getType() == SecurityType.STOCK) {
                currentPrice = marketDataService.getCurrentPrice(security.getTicker());
            } else {
                String underlyingTicker = extractUnderlyingTicker(security.getTicker());
                BigDecimal underlyingPrice = marketDataService.getCurrentPrice(underlyingTicker);
                
                if (underlyingPrice.compareTo(BigDecimal.ZERO) > 0) {
                    currentPrice = optionPricingService.calculateOptionPrice(security, underlyingPrice);
                } else {
                    currentPrice = BigDecimal.ZERO;
                }
            }
            
            position.setCurrentPrice(currentPrice);
            position.setMarketValue(position.calculateMarketValue());
        } finally {
            portfolioLock.writeLock().unlock();
        }
    }
    
    /**
     * Extracts the underlying stock ticker from an option ticker
     * Example: "AAPL-OCT-2020-110-C" -> "AAPL"
     */
    private String extractUnderlyingTicker(String optionTicker) {
        int firstDashIndex = optionTicker.indexOf('-');
        if (firstDashIndex > 0) {
            return optionTicker.substring(0, firstDashIndex);
        }
        return optionTicker;
    }
    
    /**
     * Simulates market data update and recalculates portfolio
     * WRITE operation - requires exclusive access
     */
    public void updateMarketDataAndRecalculate(Portfolio portfolio) {
        portfolioLock.writeLock().lock();
        try {
            // Update stock prices using geometric Brownian motion
            List<Position> positions = portfolio.getPositions();
            for (Position position : positions) {
                Security security = position.getSecurity();
                if (security != null && security.getType() == SecurityType.STOCK) {
                    marketDataService.simulateNextPrice(security.getTicker(), security);
                }
            }
            
            // Recalculate all position values
            calculatePortfolioValues(portfolio);
        } finally {
            portfolioLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets a summary of the portfolio
     * READ operation - multiple threads can access simultaneously
     */
    public String getPortfolioSummary(Portfolio portfolio) {
        portfolioLock.readLock().lock();
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("=== Portfolio Summary ===\n");
            summary.append("Total Positions: ").append(portfolio.getPositionCount()).append("\n");
            summary.append("Total NAV: $").append(portfolio.getTotalNAV().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
            summary.append("Last Updated: ").append(portfolio.getLastUpdated()).append("\n\n");
            
            summary.append("=== Position Details ===\n");
            for (Position position : portfolio.getPositions()) {
                summary.append(String.format("%-20s | %10s | $%10s | $%12s\n",
                        position.getSymbol(),
                        position.getPositionSize().setScale(0, BigDecimal.ROUND_HALF_UP),
                        position.getCurrentPrice().setScale(2, BigDecimal.ROUND_HALF_UP),
                        position.getMarketValue().setScale(2, BigDecimal.ROUND_HALF_UP)));
            }
            
            return summary.toString();
        } finally {
            portfolioLock.readLock().unlock();
        }
    }
    
    /**
     * Gets a summary of the portfolio with change indicators
     * READ operation - multiple threads can access simultaneously
     */
    public String getPortfolioSummaryWithChanges(Portfolio portfolio, Map<String, BigDecimal> previousStockPrices, Map<String, BigDecimal> previousOptionPrices) {
        return getPortfolioSummaryWithChanges(portfolio, previousStockPrices, previousOptionPrices, false);
    }
    
    /**
     * Gets a summary of the portfolio with change indicators
     * READ operation - multiple threads can access simultaneously
     * @param isInitial true for initial portfolio display, false for updates
     */
    public String getPortfolioSummaryWithChanges(Portfolio portfolio, Map<String, BigDecimal> previousStockPrices, Map<String, BigDecimal> previousOptionPrices, boolean isInitial) {
        portfolioLock.readLock().lock();
        try {
            StringBuilder summary = new StringBuilder();
            
            if (isInitial) {
                summary.append("=== INITIAL PORTFOLIO SUMMARY ===\n");
                summary.append("Total Positions: ").append(portfolio.getPositionCount()).append("\n");
                summary.append("Total NAV: $").append(portfolio.getTotalNAV().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
                summary.append("Initialized: ").append(portfolio.getLastUpdated()).append("\n");
                summary.append("Status: All positions marked as NEW (first time display)\n\n");
            } else {
                summary.append("=== PORTFOLIO UPDATE (Price Changes Detected) ===\n");
                summary.append("Total Positions: ").append(portfolio.getPositionCount()).append("\n");
                summary.append("Total NAV: $").append(portfolio.getTotalNAV().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
                summary.append("Last Updated: ").append(portfolio.getLastUpdated()).append("\n");
                
                // Count changes
                int upCount = 0, downCount = 0, newCount = 0;
                for (Position position : portfolio.getPositions()) {
                    String symbol = position.getSymbol();
                    BigDecimal currentPrice = position.getCurrentPrice();
                    BigDecimal previousPrice = null;
                    
                    if (position.getSecurity().getType().name().equals("STOCK")) {
                        previousPrice = previousStockPrices.get(symbol);
                    } else {
                        previousPrice = previousOptionPrices.get(symbol);
                    }
                    
                    if (previousPrice == null) {
                        newCount++;
                    } else {
                        int comparison = currentPrice.compareTo(previousPrice);
                        if (comparison > 0) upCount++;
                        else if (comparison < 0) downCount++;
                    }
                }
                
                summary.append("Changes: ").append(upCount).append(" UP, ").append(downCount).append(" DOWN, ").append(newCount).append(" NEW\n\n");
            }
            
            summary.append("=== Position Details ===\n");
            for (Position position : portfolio.getPositions()) {
                String symbol = position.getSymbol();
                BigDecimal currentPrice = position.getCurrentPrice();
                BigDecimal previousPrice = null;
                String changeIndicator = "";
                
                // Determine previous price and change indicator
                if (position.getSecurity().getType().name().equals("STOCK")) {
                    previousPrice = previousStockPrices.get(symbol);
                } else {
                    previousPrice = previousOptionPrices.get(symbol);
                }
                
                if (isInitial) {
                    // For initial display, all positions are NEW
                    changeIndicator = " [NEW]";
                } else if (previousPrice != null) {
                    int comparison = currentPrice.compareTo(previousPrice);
                    if (comparison > 0) {
                        BigDecimal change = currentPrice.subtract(previousPrice);
                        BigDecimal percentChange = change.divide(previousPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
                        changeIndicator = String.format(" [UP +$%.2f (+%.2f%%)]", change, percentChange);
                    } else if (comparison < 0) {
                        BigDecimal change = currentPrice.subtract(previousPrice);
                        BigDecimal percentChange = change.divide(previousPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
                        changeIndicator = String.format(" [DOWN $%.2f (%.2f%%)]", change.abs(), percentChange.abs());
                    } else {
                        changeIndicator = " [SAME]"; // No change (shouldn't happen with our logic)
                    }
                } else {
                    changeIndicator = " [NEW]"; // New price (first time)
                }
                
                summary.append(String.format("%-20s | %10s | $%10s | $%12s%s\n",
                        position.getSymbol(),
                        position.getPositionSize().setScale(0, BigDecimal.ROUND_HALF_UP),
                        position.getCurrentPrice().setScale(2, BigDecimal.ROUND_HALF_UP),
                        position.getMarketValue().setScale(2, BigDecimal.ROUND_HALF_UP),
                        changeIndicator));
            }
            
            return summary.toString();
        } finally {
            portfolioLock.readLock().unlock();
        }
    }
}
