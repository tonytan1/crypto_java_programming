package com.portfolio.service;

import com.portfolio.model.Portfolio;
import com.portfolio.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
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
        
        logger.info("Portfolio initialized with {} positions", portfolio.getPositionCount());
        logger.info("Initial portfolio summary:\n{}", portfolioCalculationService.getPortfolioSummary(portfolio));
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
     * Displays the current portfolio summary
     */
    private void displayPortfolioSummary() {
        Portfolio portfolio = portfolioRef.get();
        if (portfolio == null) {
            logger.warn("Portfolio not available for summary display");
            return;
        }
        
        String separator = "=================================================================================";
        String summary = portfolioCalculationService.getPortfolioSummary(portfolio);
        
        // Display in console for immediate visibility
        System.out.println("\n" + separator);
        System.out.println(summary);
        System.out.println(separator);
        
        // Also log it
        logger.info("\n{}\n{}\n{}", separator, summary, separator);
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
     * Shuts down the service
     */
    public void shutdown() {
        stopRealTimeMonitoring();
    }
}
