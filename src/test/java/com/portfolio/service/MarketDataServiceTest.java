package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import com.portfolio.marketdata.MarketDataProtos;
import com.portfolio.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketDataService
 */
public class MarketDataServiceTest {

    @Mock
    private SecurityRepository securityRepository;
    
    @Mock
    private EventPublisher eventPublisher;

    private MarketDataService marketDataService;
    private List<Security> testStocks;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        marketDataService = new MarketDataService();
        
        try {
            java.lang.reflect.Field repositoryField = MarketDataService.class.getDeclaredField("securityRepository");
            repositoryField.setAccessible(true);
            repositoryField.set(marketDataService, securityRepository);
            
            java.lang.reflect.Field eventPublisherField = MarketDataService.class.getDeclaredField("eventPublisher");
            eventPublisherField.setAccessible(true);
            eventPublisherField.set(marketDataService, eventPublisher);
            
            // Set up configuration values to prevent null pointer exceptions
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
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
        
        testStocks = createTestStocks();
        when(securityRepository.findByType(SecurityType.STOCK)).thenReturn(testStocks);
    }

    private List<Security> createTestStocks() {
        List<Security> stocks = new ArrayList<>();
        
        Security aapl = new Security();
        aapl.setTicker("AAPL");
        aapl.setType(SecurityType.STOCK);
        aapl.setMu(new BigDecimal("0.10"));
        aapl.setSigma(new BigDecimal("0.25"));
        stocks.add(aapl);
        
        Security tsla = new Security();
        tsla.setTicker("TSLA");
        tsla.setType(SecurityType.STOCK);
        tsla.setMu(new BigDecimal("0.15"));
        tsla.setSigma(new BigDecimal("0.40"));
        stocks.add(tsla);
        
        Security msft = new Security();
        msft.setTicker("MSFT");
        msft.setType(SecurityType.STOCK);
        msft.setMu(new BigDecimal("0.08"));
        msft.setSigma(new BigDecimal("0.20"));
        stocks.add(msft);
        
        return stocks;
    }

    @AfterEach
    public void tearDown() {
        // Reset mocks to clear any state
        reset(securityRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should initialize prices correctly")
    public void testInitializePrices() {
        marketDataService.initializePrices();
        
        // Verify all stocks have initial prices
        for (Security stock : testStocks) {
            BigDecimal price = marketDataService.getCurrentPrice(stock.getTicker());
            assertNotNull(price);
            assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "Price should be positive for " + stock.getTicker());
        }
        
        // Verify repository was called
        verify(securityRepository).findByType(SecurityType.STOCK);
    }

    @Test
    @DisplayName("Should get current price for existing ticker")
    public void testGetCurrentPrice() {
        marketDataService.initializePrices();
        
        BigDecimal aaplPrice = marketDataService.getCurrentPrice("AAPL");
        assertNotNull(aaplPrice);
        assertTrue(aaplPrice.compareTo(BigDecimal.ZERO) > 0);
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
        marketDataService.initializePrices();
        
        Map<String, BigDecimal> allPrices = marketDataService.getAllCurrentPrices();
        
        assertNotNull(allPrices);
        assertEquals(testStocks.size(), allPrices.size());
        
        for (Security stock : testStocks) {
            assertTrue(allPrices.containsKey(stock.getTicker()));
            assertTrue(allPrices.get(stock.getTicker()).compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    @DisplayName("Should simulate price changes using geometric Brownian motion")
    public void testSimulateNextPrice() {
        marketDataService.initializePrices();
        
        Security aapl = testStocks.get(0);
        
        // Simulate multiple price changes
        for (int i = 0; i < 10; i++) {
            BigDecimal newPrice = marketDataService.simulateNextPrice("AAPL", aapl);
            
            assertNotNull(newPrice);
            assertTrue(newPrice.compareTo(BigDecimal.ZERO) > 0, "Price should remain positive");
        }
    }

    @Test
    @DisplayName("Should handle simulation for non-existent ticker")
    public void testSimulateNextPriceForNonExistentTicker() {
        Security nonExistent = new Security();
        nonExistent.setTicker("NONEXISTENT");
        nonExistent.setType(SecurityType.STOCK);
        nonExistent.setMu(new BigDecimal("0.10"));
        nonExistent.setSigma(new BigDecimal("0.25"));
        
        BigDecimal price = marketDataService.simulateNextPrice("NONEXISTENT", nonExistent);
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    @DisplayName("Should reset prices to initial values")
    public void testResetPrices() {
        marketDataService.initializePrices();
        
        // Get initial prices
        Map<String, BigDecimal> initialPrices = marketDataService.getAllCurrentPrices();
        
        // Simulate some price changes
        for (Security stock : testStocks) {
            marketDataService.simulateNextPrice(stock.getTicker(), stock);
        }
        
        // Reset prices
        marketDataService.resetPrices();
        
        // Verify prices are back to initial values
        Map<String, BigDecimal> resetPrices = marketDataService.getAllCurrentPrices();
        for (String ticker : initialPrices.keySet()) {
            assertEquals(initialPrices.get(ticker), resetPrices.get(ticker));
        }
    }


    @Test
    @DisplayName("Should create market data snapshot")
    public void testCreateMarketDataSnapshot() {
        marketDataService.initializePrices();
        
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("100.00"));
        previousPrices.put("TSLA", new BigDecimal("200.00"));
        
        MarketDataProtos.MarketDataSnapshot snapshot = marketDataService.createMarketDataSnapshot(previousPrices);
        
        assertNotNull(snapshot);
        assertTrue(snapshot.getSnapshotTime() > 0);
        assertEquals(testStocks.size(), snapshot.getTotalSecurities());
        assertEquals(testStocks.size(), snapshot.getUpdatesCount());
        
        // Verify individual updates
        for (MarketDataProtos.MarketDataUpdate update : snapshot.getUpdatesList()) {
            assertNotNull(update.getTicker());
            assertTrue(update.getPrice() > 0);
            assertNotNull(update.getPriceChange());
            assertNotNull(update.getSource());
        }
    }

    @Test
    @DisplayName("Should create market data update")
    public void testCreateMarketDataUpdate() {
        marketDataService.initializePrices();
        
        BigDecimal previousPrice = new BigDecimal("100.00");
        MarketDataProtos.MarketDataUpdate update = marketDataService.createMarketDataUpdate("AAPL", previousPrice);
        
        assertNotNull(update);
        assertEquals("AAPL", update.getTicker());
        assertTrue(update.getPrice() > 0);
        assertNotNull(update.getPriceChange());
        assertNotNull(update.getSource());
    }


    @Test
    @DisplayName("Should serialize market data")
    public void testSerializeMarketData() {
        marketDataService.initializePrices();
        
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("100.00"));
        
        byte[] serializedData = marketDataService.serializeMarketData(previousPrices);
        
        assertNotNull(serializedData);
        assertTrue(serializedData.length > 0);
    }

    @Test
    @DisplayName("Should handle different volatility levels")
    public void testDifferentVolatilityLevels() {
        marketDataService.initializePrices();
        
        // Test with high volatility stock
        Security highVolStock = testStocks.get(1); // TSLA with 40% volatility
        BigDecimal highVolPrice = marketDataService.simulateNextPrice("TSLA", highVolStock);
        
        // Test with low volatility stock  
        Security lowVolStock = testStocks.get(2); // MSFT with 20% volatility
        BigDecimal lowVolPrice = marketDataService.simulateNextPrice("MSFT", lowVolStock);
        
        assertNotNull(highVolPrice);
        assertNotNull(lowVolPrice);
        assertTrue(highVolPrice.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(lowVolPrice.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should maintain price positivity")
    public void testPricePositivity() {
        marketDataService.initializePrices();
        
        // Simulate many price changes to ensure prices stay positive
        for (int i = 0; i < 100; i++) {
            for (Security stock : testStocks) {
                BigDecimal price = marketDataService.simulateNextPrice(stock.getTicker(), stock);
                assertTrue(price.compareTo(BigDecimal.ZERO) > 0, 
                    "Price should remain positive for " + stock.getTicker() + " at iteration " + i);
            }
        }
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    public void testConcurrentAccess() throws InterruptedException {
        marketDataService.initializePrices();
        
        int numThreads = 10;
        int iterationsPerThread = 100;
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        for (Security stock : testStocks) {
                            marketDataService.simulateNextPrice(stock.getTicker(), stock);
                            marketDataService.getCurrentPrice(stock.getTicker());
                        }
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
        
        // Verify all prices are still positive
        for (Security stock : testStocks) {
            BigDecimal price = marketDataService.getCurrentPrice(stock.getTicker());
            assertTrue(price.compareTo(BigDecimal.ZERO) > 0);
        }
    }
}

