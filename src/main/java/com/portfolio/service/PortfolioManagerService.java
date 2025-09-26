package com.portfolio.service;

import com.portfolio.model.Portfolio;
import com.portfolio.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main service that orchestrates the portfolio management system.
 * Handles loading positions, starting market data simulation, and portfolio calculations.
 */
@Service
public class PortfolioManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioManagerService.class);
    
    @Autowired
    private PositionLoaderService positionLoaderService;
    
    @Autowired
    private PortfolioCalculationService portfolioCalculationService;
    
    @Autowired
    private MarketDataService marketDataService;
    
    // Thread-safe portfolio state using AtomicReference
    private final AtomicReference<Portfolio> portfolioRef = new AtomicReference<>();
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    // Price change tracking for smart display
    private final Map<String, BigDecimal> previousPrices = new HashMap<>();
    private final Map<String, BigDecimal> previousOptionPrices = new HashMap<>();
    
    /**
     * Initializes the portfolio by loading positions from CSV
     */
    public void initializePortfolio() throws IOException {
        logger.info("Initializing portfolio...");
        
        // Load positions from CSV
        List<Position> positions = positionLoaderService.loadPositions();
        
        // Validate positions
        List<String> missingSecurities = positionLoaderService.validatePositions(positions);
        if (!missingSecurities.isEmpty()) {
            logger.warn("Missing security definitions for: {}", missingSecurities);
        }
        
        // Create portfolio
        Portfolio portfolio = new Portfolio(positions);
        
        // Calculate initial values
        portfolioCalculationService.calculatePortfolioValues(portfolio);
        
        // Set portfolio in atomic reference
        portfolioRef.set(portfolio);
        
        // Initialize previous prices for change tracking
        updatePreviousPrices(portfolio);
        
        logger.info("Portfolio initialized with {} positions", portfolio.getPositionCount());
        
        // Display initial summary with "new" indicators
        String separator = "=================================================================================";
        String initialSummary = portfolioCalculationService.getPortfolioSummaryWithChanges(portfolio, previousPrices, previousOptionPrices);
        System.out.println("\n" + separator);
        System.out.println(initialSummary);
        System.out.println(separator);
        logger.info("Initial portfolio summary:\n{}", initialSummary);
    }
    
    /**
     * Starts the real-time portfolio monitoring
     */
    public void startRealTimeMonitoring() {
        if (isRunning) {
            logger.info("Portfolio monitoring is already running");
            return;
        }
        
        if (portfolioRef.get() == null) {
            logger.error("Portfolio not initialized. Call initializePortfolio() first.");
            return;
        }
        
        logger.info("Starting real-time portfolio monitoring...");
        isRunning = true;
        
        // Create scheduler for periodic updates
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Schedule market data updates and portfolio recalculation
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePortfolio();
            } catch (Exception e) {
                logger.error("Error updating portfolio: {}", e.getMessage(), e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // Schedule portfolio summary display
        scheduler.scheduleAtFixedRate(() -> {
            try {
                displayPortfolioSummary();
            } catch (Exception e) {
                logger.error("Error displaying portfolio summary: {}", e.getMessage(), e);
            }
        }, 2, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Stops the real-time monitoring
     */
    public void stopRealTimeMonitoring() {
        if (!isRunning) {
            logger.info("Portfolio monitoring is not running");
            return;
        }
        
        logger.info("Stopping real-time portfolio monitoring...");
        isRunning = false;
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Updates the portfolio with new market data
     */
    private void updatePortfolio() {
        Portfolio portfolio = portfolioRef.get();
        if (portfolio != null) {
            portfolioCalculationService.updateMarketDataAndRecalculate(portfolio);
        }
    }
    
    /**
     * Displays the current portfolio summary only when prices change
     */
    private void displayPortfolioSummary() {
        Portfolio portfolio = portfolioRef.get();
        if (portfolio == null) {
            logger.warn("Portfolio not available for summary display");
            return;
        }
        
        // Check if any prices have changed
        boolean hasChanges = checkForPriceChanges(portfolio);
        
        if (!hasChanges) {
            // No changes detected, skip display
            return;
        }
        
        String separator = "=================================================================================";
        String summary = portfolioCalculationService.getPortfolioSummaryWithChanges(portfolio, previousPrices, previousOptionPrices);
        
        // Display in console for immediate visibility
        System.out.println("\n" + separator);
        System.out.println(summary);
        System.out.println(separator);
        
        // Also log it
        logger.info("\n{}\n{}\n{}", separator, summary, separator);
        
        // Update previous prices for next comparison
        updatePreviousPrices(portfolio);
    }
    
    /**
     * Gets the current portfolio
     */
    public Portfolio getPortfolio() {
        return portfolioRef.get();
    }
    
    /**
     * Checks if monitoring is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Checks if any prices have changed since last display
     */
    private boolean checkForPriceChanges(Portfolio portfolio) {
        boolean hasChanges = false;
        
        for (Position position : portfolio.getPositions()) {
            String symbol = position.getSymbol();
            BigDecimal currentPrice = position.getCurrentPrice();
            
            if (position.getSecurity().getType().name().equals("STOCK")) {
                BigDecimal previousPrice = previousPrices.get(symbol);
                if (previousPrice == null || currentPrice.compareTo(previousPrice) != 0) {
                    hasChanges = true;
                    break;
                }
            } else {
                // For options, check option prices
                BigDecimal previousPrice = previousOptionPrices.get(symbol);
                if (previousPrice == null || currentPrice.compareTo(previousPrice) != 0) {
                    hasChanges = true;
                    break;
                }
            }
        }
        
        return hasChanges;
    }
    
    /**
     * Updates the previous prices for next comparison
     */
    private void updatePreviousPrices(Portfolio portfolio) {
        for (Position position : portfolio.getPositions()) {
            String symbol = position.getSymbol();
            BigDecimal currentPrice = position.getCurrentPrice();
            
            if (position.getSecurity().getType().name().equals("STOCK")) {
                previousPrices.put(symbol, currentPrice);
            } else {
                previousOptionPrices.put(symbol, currentPrice);
            }
        }
    }
    
    /**
     * Shuts down the service
     */
    public void shutdown() {
        stopRealTimeMonitoring();
    }
}
