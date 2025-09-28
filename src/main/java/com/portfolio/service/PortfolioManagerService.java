package com.portfolio.service;

import com.portfolio.event.EventPublisher;
import com.portfolio.events.PortfolioEventProtos;
import com.portfolio.model.Portfolio;
import com.portfolio.model.Position;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main service that orchestrates the portfolio management system.
 * Handles loading positions, starting market data simulation, and portfolio calculations.
 */
@Service
public class PortfolioManagerService {
    
    private static final Logger logger = Logger.getLogger(PortfolioManagerService.class.getName());
    
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
    private final Map<String, BigDecimal> initialPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> initialOptionPrices = new ConcurrentHashMap<>();
    
    @Value("${portfolio.portfolio-manager.scheduler.core-pool-size:2}")
    private int schedulerCorePoolSize;
    
    @Value("${portfolio.portfolio-manager.scheduler.thread-name-prefix:portfolio-scheduler-}")
    private String schedulerThreadNamePrefix;
    
    @Value("${portfolio.portfolio-manager.scheduler.wait-for-tasks-to-complete-on-shutdown:true}")
    private boolean schedulerWaitForTasksToCompleteOnShutdown;
    
    @Value("${portfolio.portfolio-manager.scheduler.await-termination-seconds:10}")
    private int schedulerAwaitTerminationSeconds;
    
    private ThreadPoolTaskScheduler scheduler;
    private boolean isRunning = false;
    
    /**
     * Initializes the scheduler after Spring dependency injection.
     */
    @PostConstruct
    public void initializeScheduler() {
        this.scheduler = createScheduler();
    }
    
    /**
     * Creates a ThreadPoolTaskScheduler configured via YAML properties.
     */
    private ThreadPoolTaskScheduler createScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerCorePoolSize);
        scheduler.setThreadNamePrefix(schedulerThreadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(schedulerWaitForTasksToCompleteOnShutdown);
        scheduler.setAwaitTerminationSeconds(schedulerAwaitTerminationSeconds);
        scheduler.initialize();
        return scheduler;
    }
    
    /**
     * Initializes the portfolio by loading positions from CSV
     */
    public void initializePortfolio() throws IOException {
        logger.info("Initializing portfolio...");
        
        List<Position> positions = positionLoaderService.loadPositions();
        List<String> missingSecurities = positionLoaderService.validatePositions(positions);
        if (!missingSecurities.isEmpty()) {
            logger.warning("Missing security definitions for: " + missingSecurities);
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
        logger.info("Portfolio initialized with " + portfolio.getPositionCount() + " positions");
        
        // Display initial summary with "new" indicators (before setting initial prices)
        String separator = "=================================================================================";
        String initialSummary = portfolioCalculationService.getPortfolioSummaryWithChanges(portfolio, initialPrices, initialOptionPrices, true);
        logger.info("\n" + separator + "\n" + initialSummary + "\n" + separator);
        setInitialPrices(portfolio);
    }
    
    public void startRealTimeMonitoring() {
        if (isRunning) {
            logger.info("Portfolio monitoring is already running");
            return;
        }
        
        if (portfolioRef.get() == null) {
            logger.severe("Portfolio not initialized. Call initializePortfolio() first.");
            return;
        }
        
        logger.info("Starting real-time portfolio monitoring...");
        isRunning = true;
        
        // Schedule portfolio updates every 1 second
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePortfolio();
            } catch (Exception e) {
                logger.severe("Error updating portfolio: " + e.getMessage());
            }
        }, Duration.ofSeconds(1));
        
        // Schedule portfolio summary display every 5 seconds (starting after 2 seconds delay)
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                displayPortfolioSummary();
            } catch (Exception e) {
                logger.severe("Error displaying portfolio summary: " + e.getMessage());
            }
        }, java.time.Instant.now().plusSeconds(2), Duration.ofSeconds(5));
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
        }
    }
    
    /**
     * Clean shutdown of the scheduler when the application stops.
     */
    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            logger.info("Shutting down portfolio manager scheduler...");
            scheduler.shutdown();
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
            logger.warning("Portfolio not available for summary display");
            return;
        }
        
        // Check if any prices have changed
        boolean hasChanges = checkForPriceChanges(portfolio);
        logger.fine("Price change check result: hasChanges = " + hasChanges);
        
        if (!hasChanges) {
            // No changes detected, skip display
            logger.fine("No price changes detected, skipping portfolio summary display");
            return;
        }
        
        String separator = "=================================================================================";
        String summary = portfolioCalculationService.getPortfolioSummaryWithChanges(portfolio, initialPrices, initialOptionPrices);
        
        // Log the portfolio summary
        logger.info("\n" + separator + "\n" + summary + "\n" + separator);
        
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
                logger.fine("Skipping position " + symbol + " - no security definition for change check");
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
                logger.warning("Skipping position " + symbol + " - no security definition");
                continue;
            }
            
            if (position.getSecurity().getType().name().equals("STOCK")) {
                initialPrices.put(symbol, currentPrice);
            } else {
                initialOptionPrices.put(symbol, currentPrice);
            }
        }
        logger.info("Initial prices set for " + initialPrices.size() + " stocks and " + initialOptionPrices.size() + " options");
    }
    
}
