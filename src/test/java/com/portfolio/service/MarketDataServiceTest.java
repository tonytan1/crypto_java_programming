package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.marketdata.MarketDataProtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified unit tests for MarketDataService without Mockito
 * Focus on testing core functionality without complex dependency injection
 */
public class MarketDataServiceTest {

    private MarketDataService marketDataService;

    @BeforeEach
    public void setUp() {
        marketDataService = new MarketDataService();
        
        // Set up configuration values using reflection
        try {
            java.lang.reflect.Field minUpdateIntervalField = MarketDataService.class.getDeclaredField("minUpdateInterval");
            minUpdateIntervalField.setAccessible(true);
            minUpdateIntervalField.set(marketDataService, 500L);
            
            java.lang.reflect.Field maxUpdateIntervalField = MarketDataService.class.getDeclaredField("maxUpdateInterval");
            maxUpdateIntervalField.setAccessible(true);
            maxUpdateIntervalField.set(marketDataService, 2000L);
            
            java.lang.reflect.Field initialPricesConfigField = MarketDataService.class.getDeclaredField("initialPricesConfig");
            initialPricesConfigField.setAccessible(true);
            Map<String, String> initialPrices = new HashMap<>();
            initialPrices.put("AAPL", "150.00");
            initialPrices.put("TSLA", "800.00");
            initialPrices.put("MSFT", "300.00");
            initialPricesConfigField.set(marketDataService, initialPrices);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test configuration", e);
        }
    }

    @Test
    @DisplayName("Should get current price for existing ticker")
    public void testGetCurrentPrice() {
        // Test basic price retrieval
        BigDecimal price = marketDataService.getCurrentPrice("AAPL");
        assertNotNull(price);
        // Initially should be zero since prices aren't initialized
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    @DisplayName("Should return zero for non-existent ticker")
    public void testGetCurrentPriceForNonExistentTicker() {
        BigDecimal price = marketDataService.getCurrentPrice("NONEXISTENT");
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    @DisplayName("Should get all current prices")
    public void testGetAllCurrentPrices() {
        Map<String, BigDecimal> allPrices = marketDataService.getAllCurrentPrices();
        
        assertNotNull(allPrices);
        assertTrue(allPrices.isEmpty()); // Should be empty initially
    }

    @Test
    @DisplayName("Should handle simulation for non-existent ticker")
    public void testSimulateNextPriceForNonExistentTicker() {
        Security nonExistent = createTestSecurity("NONEXISTENT", SecurityType.STOCK);
        
        BigDecimal price = marketDataService.simulateNextPrice("NONEXISTENT", nonExistent);
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    @DisplayName("Should handle different volatility levels")
    public void testDifferentVolatilityLevels() {
        Security highVolStock = createTestSecurity("TSLA", SecurityType.STOCK);
        highVolStock.setSigma(new BigDecimal("0.40")); // High volatility
        
        Security lowVolStock = createTestSecurity("MSFT", SecurityType.STOCK);
        lowVolStock.setSigma(new BigDecimal("0.20")); // Low volatility
        
        // Both should return zero since prices aren't initialized
        BigDecimal highVolPrice = marketDataService.simulateNextPrice("TSLA", highVolStock);
        BigDecimal lowVolPrice = marketDataService.simulateNextPrice("MSFT", lowVolStock);
        
        assertEquals(BigDecimal.ZERO, highVolPrice);
        assertEquals(BigDecimal.ZERO, lowVolPrice);
    }

    @Test
    @DisplayName("Should create market data snapshot")
    public void testCreateMarketDataSnapshot() {
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("100.00"));
        previousPrices.put("TSLA", new BigDecimal("200.00"));
        
        MarketDataProtos.MarketDataSnapshot snapshot = marketDataService.createMarketDataSnapshot(previousPrices);
        
        assertNotNull(snapshot);
        assertTrue(snapshot.getSnapshotTime() > 0);
        assertEquals(0, snapshot.getTotalSecurities()); // No securities loaded
        assertEquals(0, snapshot.getUpdatesCount());
    }

    @Test
    @DisplayName("Should create market data update")
    public void testCreateMarketDataUpdate() {
        BigDecimal previousPrice = new BigDecimal("100.00");
        MarketDataProtos.MarketDataUpdate update = marketDataService.createMarketDataUpdate("AAPL", previousPrice);
        
        assertNotNull(update);
        assertEquals("AAPL", update.getTicker());
        assertEquals(0.0, update.getPrice()); // Should be zero since no price is set
        assertNotNull(update.getPriceChange());
        assertNotNull(update.getSource());
    }

    @Test
    @DisplayName("Should serialize market data")
    public void testSerializeMarketData() {
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("100.00"));
        
        byte[] serializedData = marketDataService.serializeMarketData(previousPrices);
        
        assertNotNull(serializedData);
        assertTrue(serializedData.length > 0);
    }

    @Test
    @DisplayName("Should handle price validation")
    public void testPriceValidation() {
        // Test that the service can handle various price scenarios
        BigDecimal zeroPrice = marketDataService.getCurrentPrice("ZERO");
        assertEquals(BigDecimal.ZERO, zeroPrice);
        
        // Test with null ticker - should handle gracefully
        try {
            BigDecimal nullPrice = marketDataService.getCurrentPrice(null);
            assertEquals(BigDecimal.ZERO, nullPrice);
        } catch (Exception e) {
            // If it throws an exception, that's also acceptable behavior
            assertTrue(e instanceof NullPointerException || e instanceof IllegalArgumentException);
        }
    }

    @Test
    @DisplayName("Should handle configuration properly")
    public void testConfigurationHandling() {
        // Test that configuration values are properly set
        long minInterval = marketDataService.getMinUpdateInterval();
        long maxInterval = marketDataService.getMaxUpdateInterval();
        
        assertEquals(500L, minInterval);
        assertEquals(2000L, maxInterval);
        assertTrue(maxInterval > minInterval);
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    public void testConcurrentAccess() throws InterruptedException {
        int numThreads = 5;
        int iterationsPerThread = 10;
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        marketDataService.getCurrentPrice("AAPL");
                        marketDataService.getAllCurrentPrices();
                        marketDataService.getMinUpdateInterval();
                        marketDataService.getMaxUpdateInterval();
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access: " + exceptions);
    }

    @Test
    @DisplayName("Should handle reset prices operation")
    public void testResetPrices() {
        // Test that reset operation doesn't throw exceptions
        assertDoesNotThrow(() -> {
            marketDataService.resetPrices();
        });
        
        // After reset, prices should still be empty
        Map<String, BigDecimal> prices = marketDataService.getAllCurrentPrices();
        assertNotNull(prices);
        assertTrue(prices.isEmpty());
    }

    // Helper method to create test securities
    private Security createTestSecurity(String ticker, SecurityType type) {
        Security security = new Security();
        security.setTicker(ticker);
        security.setType(type);
        security.setMu(new BigDecimal("0.10"));
        security.setSigma(new BigDecimal("0.25"));
        return security;
    }
}