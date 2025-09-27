package com.portfolio.service;

import com.portfolio.event.EventPublisher;
import com.portfolio.events.PortfolioEventProtos;
import com.portfolio.model.Portfolio;
import com.portfolio.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    @Autowired
    private EventPublisher eventPublisher;
    
    // Thread-safe portfolio state using AtomicReference
    private final AtomicReference<Portfolio> portfolioRef = new AtomicReference<>();
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private final Map<String, BigDecimal> initialPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> initialOptionPrices = new ConcurrentHashMap<>();
    
    /**
     * Initializes the portfolio by loading positions from CSV
     */
    public void initializePortfolio() throws IOException {
        logger.info("Initializing portfolio...");
        
        List<Position> positions = positionLoaderService.loadPositions();
        List<String> missingSecurities = positionLoaderService.validatePositions(positions);
        if (!missingSecurities.isEmpty()) {
            logger.warn("Missing security definitions for: {}", missingSecurities);
        }
        
        Portfolio portfolio = new Portfolio(positions);
        portfolioCalculationService.calculatePortfolioValues(portfolio);
        for (Position position : positions) {
            eventPublisher.publishPositionUpdate(
                position.getSymbol(),
                BigDecimal.ZERO,
                position.getPositionSize(),
                PortfolioEventProtos.UpdateAction.ADDED,
                "Initial portfolio load"
            );
        }
        
        portfolioRef.set(portfolio);
        logger.info("Portfolio initialized with {} positions", portfolio.getPositionCount());
        
        // Display initial summary with "new" indicators (before setting initial prices)
        String separator = "=================================================================================";
        String initialSummary = portfolioCalculationService.getPortfolioSummaryWithChanges(portfolio, initialPrices, initialOptionPrices, true);
        logger.info("\n{}\n{}\n{}", separator, initialSummary, separator);
        setInitialPrices(portfolio);
    }
    
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
        
        scheduler = Executors.newScheduledThreadPool(2);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePortfolio();
            } catch (Exception e) {
                logger.error("Error updating portfolio: {}", e.getMessage(), e);
            }
        }, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                displayPortfolioSummary();
            } catch (Exception e) {
                logger.error("Error displaying portfolio summary: {}", e.getMessage(), e);
            }
        }, 2, 5, TimeUnit.SECONDS);
    }
    
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
        public void updatePortfolio() {
            Portfolio portfolio = portfolioRef.get();
            if (portfolio != null) {
                portfolioCalculationService.updateMarketDataAndRecalculate(portfolio);
                
                // Log market data in Protobuf format for debugging
                marketDataService.logMarketDataSnapshot(initialPrices);
            }
        }
    
    private void displayPortfolioSummary() {
        Portfolio portfolio = portfolioRef.get();
        if (portfolio == null) {
            logger.warn("Portfolio not available for summary display");
            return;
        }
        
        // Check if any prices have changed
        boolean hasChanges = checkForPriceChanges(portfolio);
        logger.debug("Price change check result: hasChanges = {}", hasChanges);
        
        if (!hasChanges) {
            // No changes detected, skip display
            logger.debug("No price changes detected, skipping portfolio summary display");
            return;
        }
        
        String separator = "=================================================================================";
        String summary = portfolioCalculationService.getPortfolioSummaryWithChanges(portfolio, initialPrices, initialOptionPrices);
        
        // Log the portfolio summary
        logger.info("\n{}\n{}\n{}", separator, summary, separator);
        
        // Note: We don't update initial prices - they remain as the baseline for comparison
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
            
            // Skip positions without security definitions
            if (position.getSecurity() == null) {
                logger.debug("Skipping position {} - no security definition for change check", symbol);
                continue;
            }
            
            if (position.getSecurity().getType().name().equals("STOCK")) {
                BigDecimal initialPrice = initialPrices.get(symbol);
                if (initialPrice == null || currentPrice.compareTo(initialPrice) != 0) {
                    hasChanges = true;
                    break;
                }
            } else {
                // For options, check option prices
                BigDecimal initialPrice = initialOptionPrices.get(symbol);
                if (initialPrice == null || currentPrice.compareTo(initialPrice) != 0) {
                    hasChanges = true;
                    break;
                }
            }
        }
        
        return hasChanges;
    }
    
    /**
     * Sets the initial prices for baseline comparison (called only once during initialization)
     */
    private void setInitialPrices(Portfolio portfolio) {
        for (Position position : portfolio.getPositions()) {
            String symbol = position.getSymbol();
            BigDecimal currentPrice = position.getCurrentPrice();
            
            // Skip positions without security definitions
            if (position.getSecurity() == null) {
                logger.warn("Skipping position {} - no security definition", symbol);
                continue;
            }
            
            if (position.getSecurity().getType().name().equals("STOCK")) {
                initialPrices.put(symbol, currentPrice);
            } else {
                initialOptionPrices.put(symbol, currentPrice);
            }
        }
        logger.info("Initial prices set for {} stocks and {} options", initialPrices.size(), initialOptionPrices.size());
    }
    
    /**
     * Shuts down the service
     */
    public void shutdown() {
        stopRealTimeMonitoring();
    }
}
