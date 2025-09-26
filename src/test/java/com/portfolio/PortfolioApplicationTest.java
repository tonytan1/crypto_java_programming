package com.portfolio;

import com.portfolio.model.Portfolio;
import com.portfolio.model.Position;
import com.portfolio.service.PortfolioManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic integration test for the Portfolio Application
 */
public class PortfolioApplicationTest {
    
    private ApplicationContext context;
    private PortfolioManagerService portfolioManagerService;
    
    @BeforeEach
    public void setUp() {
        // Create a fresh Spring context for each test
        context = new AnnotationConfigApplicationContext(PortfolioApplication.class);
        portfolioManagerService = context.getBean(PortfolioManagerService.class);
    }
    
    @Test
    public void testPortfolioInitialization() throws IOException {
        // Initialize portfolio
        portfolioManagerService.initializePortfolio();
        
        // Verify portfolio is created
        Portfolio portfolio = portfolioManagerService.getPortfolio();
        assertNotNull(portfolio);
        assertTrue(portfolio.getPositionCount() > 0);
        
        // Verify positions are loaded
        List<Position> positions = portfolio.getPositions();
        assertFalse(positions.isEmpty());
        
        // Verify at least one position has a security definition
        boolean hasSecurityDefinition = positions.stream()
                .anyMatch(p -> p.getSecurity() != null);
        assertTrue(hasSecurityDefinition, "At least one position should have a security definition");
    }
    
    @Test
    public void testPortfolioCalculation() throws IOException {
        // Initialize portfolio
        portfolioManagerService.initializePortfolio();
        
        // Calculate values
        Portfolio portfolio = portfolioManagerService.getPortfolio();
        assertNotNull(portfolio.getTotalNAV());
        assertNotNull(portfolio.getLastUpdated());
    }
}
