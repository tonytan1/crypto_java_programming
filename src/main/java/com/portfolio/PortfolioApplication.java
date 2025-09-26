package com.portfolio;

import com.portfolio.service.PortfolioManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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

    public static void main(String[] args) {
        try {
            logger.info("=== PORTFOLIO APPLICATION STARTING ===");
            logger.info("Starting Portfolio Application...");
            logger.info("Logging system initialized successfully");
            
            // Initialize Spring context
            ApplicationContext context = new AnnotationConfigApplicationContext(PortfolioApplication.class);
            PortfolioManagerService portfolioManagerService = context.getBean(PortfolioManagerService.class);
            
            // Initialize portfolio
            portfolioManagerService.initializePortfolio();
            
            // Start real-time monitoring
            portfolioManagerService.startRealTimeMonitoring();
            
            // Keep the application running
            logger.info("Portfolio application is running. Press Ctrl+C to stop.");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down portfolio application...");
                portfolioManagerService.shutdown();
            }));
            
            // Keep main thread alive
            while (portfolioManagerService.isRunning()) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            logger.error("Failed to initialize portfolio: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
        }
    }
}
