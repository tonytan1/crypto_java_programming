package com.portfolio;

import com.portfolio.event.EventBus;
import com.portfolio.event.EventPublisher;
import com.portfolio.event.listener.ConsoleEventListener;
import com.portfolio.service.DataInitializationService;
import com.portfolio.service.MarketDataService;
import com.portfolio.service.PortfolioManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Main application class for the Real-Time Portfolio Valuation System.
 * 
 * This application provides real-time portfolio valuation capabilities for:
 * - Common stocks
 * - European Call options
 * - European Put options
 */
@Configuration
@ComponentScan(basePackages = "com.portfolio")
public class PortfolioApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioApplication.class);
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private ConsoleEventListener consoleListener;
    
    
    @Autowired
    private PortfolioManagerService portfolioManagerService;
    
    @Autowired
    private DataInitializationService dataInitializationService;
    
    @Autowired
    private MarketDataService marketDataService;
    
    private boolean isRunning = false;

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PortfolioApplication.class)) {
            logger.info("=== PORTFOLIO APPLICATION STARTING ===");
            logger.info("Starting Portfolio Application...");
            logger.info("Logging system initialized successfully");
            
            // Initialize Spring context
            PortfolioApplication app = context.getBean(PortfolioApplication.class);
            
            // Initialize and run the application
            app.initialize();
            app.run();
            
        } catch (Exception e) {
            logger.error("Failed to start application: {}", e.getMessage(), e);
        }
    }
    
    @PostConstruct
    public void initialize() throws IOException {
        logger.info("Initializing Portfolio Application...");
        
        // Initialize database with sample data first
        logger.info("Initializing database with sample data...");
        dataInitializationService.initializeSampleData();
        
        // Initialize market data service after data is loaded
        logger.info("Initializing market data service...");
        marketDataService.initializePrices();
        
        // Register event listeners
        eventBus.subscribe(consoleListener);
        
        logger.info("Event system initialized with {} listeners", eventBus.getListenerCount());
        
        // Publish system started event
        eventPublisher.publishSystemStarted();
        
        // Initialize portfolio
        portfolioManagerService.initializePortfolio();
        
        // Start real-time monitoring
        portfolioManagerService.startRealTimeMonitoring();
        
        isRunning = true;
        logger.info("Portfolio application initialized successfully");
    }
    
    public void run() {
        try {
            logger.info("Portfolio application is running. Press Ctrl+C to stop.");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down portfolio application...");
                eventPublisher.publishSystemStopped();
                portfolioManagerService.shutdown();
                eventBus.shutdown();
                isRunning = false;
            }));
            
            // Keep main thread alive
            while (isRunning && portfolioManagerService.isRunning()) {
                Thread.sleep(1000);
            }
            
        } catch (InterruptedException e) {
            logger.info("Application interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Unexpected error during execution: {}", e.getMessage(), e);
        }
    }
}
