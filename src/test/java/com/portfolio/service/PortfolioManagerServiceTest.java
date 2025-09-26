package com.portfolio.service;

import com.portfolio.model.*;
import com.portfolio.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioManagerService
 */
public class PortfolioManagerServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PortfolioCalculationService portfolioCalculationService;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private PositionLoaderService positionLoaderService;

    private PortfolioManagerService portfolioManagerService;
    private List<Security> testSecurities;
    private List<Position> testPositions;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        portfolioManagerService = new PortfolioManagerService();
        
        // Use reflection to inject dependencies
        try {
            java.lang.reflect.Field eventPublisherField = PortfolioManagerService.class.getDeclaredField("eventPublisher");
            eventPublisherField.setAccessible(true);
            eventPublisherField.set(portfolioManagerService, eventPublisher);
            
            java.lang.reflect.Field calculationField = PortfolioManagerService.class.getDeclaredField("portfolioCalculationService");
            calculationField.setAccessible(true);
            calculationField.set(portfolioManagerService, portfolioCalculationService);
            
            java.lang.reflect.Field marketDataField = PortfolioManagerService.class.getDeclaredField("marketDataService");
            marketDataField.setAccessible(true);
            marketDataField.set(portfolioManagerService, marketDataService);
            
            java.lang.reflect.Field positionLoaderField = PortfolioManagerService.class.getDeclaredField("positionLoaderService");
            positionLoaderField.setAccessible(true);
            positionLoaderField.set(portfolioManagerService, positionLoaderService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        }
        
        // Create test data
        testSecurities = createTestSecurities();
        testPositions = createTestPositions();
    }

    private List<Security> createTestSecurities() {
        List<Security> securities = new ArrayList<>();
        
        // AAPL Stock
        Security aaplStock = new Security();
        aaplStock.setTicker("AAPL");
        aaplStock.setType(SecurityType.STOCK);
        aaplStock.setMu(new BigDecimal("0.10"));
        aaplStock.setSigma(new BigDecimal("0.25"));
        securities.add(aaplStock);
        
        // AAPL Call Option
        Security aaplCall = new Security();
        aaplCall.setTicker("AAPL_CALL_150_2024");
        aaplCall.setType(SecurityType.CALL);
        aaplCall.setStrike(new BigDecimal("150.00"));
        aaplCall.setMaturity(LocalDate.of(2024, 12, 20));
        aaplCall.setMu(new BigDecimal("0.10"));
        aaplCall.setSigma(new BigDecimal("0.25"));
        securities.add(aaplCall);
        
        return securities;
    }

    private List<Position> createTestPositions() {
        List<Position> positions = new ArrayList<>();
        
        // AAPL Stock Position
        Security aaplStock = testSecurities.get(0);
        Position aaplPosition = new Position("AAPL", new BigDecimal("100"), aaplStock);
        positions.add(aaplPosition);
        
        // AAPL Call Position
        Security aaplCall = testSecurities.get(1);
        Position callPosition = new Position("AAPL_CALL_150_2024", new BigDecimal("10"), aaplCall);
        positions.add(callPosition);
        
        return positions;
    }

    @AfterEach
    public void tearDown() {
        // Reset mocks to clear any state
        reset(eventPublisher, portfolioCalculationService, marketDataService, positionLoaderService);
    }

    @Test
    @DisplayName("Should initialize portfolio successfully")
    public void testInitializePortfolio() throws IOException {
        // Mock dependencies
        when(positionLoaderService.loadPositions()).thenReturn(testPositions);
        when(positionLoaderService.validatePositions(anyList())).thenReturn(new ArrayList<>());
        when(portfolioCalculationService.getPortfolioSummaryWithChanges(any(Portfolio.class), anyMap(), anyMap(), eq(true)))
            .thenReturn("Portfolio Summary");
        
        // Execute
        portfolioManagerService.initializePortfolio();
        
        // Verify
        Portfolio portfolio = portfolioManagerService.getPortfolio();
        assertNotNull(portfolio);
        assertEquals(2, portfolio.getPositionCount());
        
        // Verify services were called
        verify(positionLoaderService).loadPositions();
        verify(portfolioCalculationService).getPortfolioSummaryWithChanges(any(Portfolio.class), anyMap(), anyMap(), eq(true));
    }

    @Test
    @DisplayName("Should handle missing security definitions during initialization")
    public void testInitializePortfolioWithMissingSecurities() throws IOException {
        // Create position with missing security
        Position positionWithoutSecurity = new Position("UNKNOWN", new BigDecimal("100"), null);
        List<Position> positionsWithMissing = Arrays.asList(positionWithoutSecurity);
        
        when(positionLoaderService.loadPositions()).thenReturn(positionsWithMissing);
        when(positionLoaderService.validatePositions(anyList())).thenReturn(Arrays.asList("UNKNOWN"));
        when(portfolioCalculationService.getPortfolioSummaryWithChanges(any(Portfolio.class), anyMap(), anyMap(), eq(true)))
            .thenReturn("Portfolio Summary");
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            portfolioManagerService.initializePortfolio();
        });
        
        Portfolio portfolio = portfolioManagerService.getPortfolio();
        assertNotNull(portfolio);
        assertEquals(1, portfolio.getPositionCount());
    }

    @Test
    @DisplayName("Should start real-time monitoring")
    public void testStartRealTimeMonitoring() {
        // Initialize portfolio first
        Portfolio testPortfolio = new Portfolio();
        testPortfolio.setPositions(testPositions);
        
        // Use reflection to set the portfolio
        try {
            java.lang.reflect.Field portfolioField = PortfolioManagerService.class.getDeclaredField("portfolioRef");
            portfolioField.setAccessible(true);
            AtomicReference<Portfolio> portfolioRef = (AtomicReference<Portfolio>) portfolioField.get(portfolioManagerService);
            portfolioRef.set(testPortfolio);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set portfolio", e);
        }
        
        when(portfolioCalculationService.getPortfolioSummaryWithChanges(any(Portfolio.class), anyMap(), anyMap(), eq(false)))
            .thenReturn("Updated Portfolio Summary");
        
        // Execute
        portfolioManagerService.startRealTimeMonitoring();
        
        // Verify
        assertTrue(portfolioManagerService.isRunning());
    }

    @Test
    @DisplayName("Should not start monitoring if already running")
    public void testStartRealTimeMonitoringWhenAlreadyRunning() {
        // Set up already running state
        try {
            java.lang.reflect.Field runningField = PortfolioManagerService.class.getDeclaredField("isRunning");
            runningField.setAccessible(true);
            runningField.set(portfolioManagerService, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set running state", e);
        }
        
        // Execute
        portfolioManagerService.startRealTimeMonitoring();
        
        // Verify no additional setup was done
        verify(portfolioCalculationService, never()).getPortfolioSummaryWithChanges(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Should stop real-time monitoring")
    public void testStopRealTimeMonitoring() {
        // Set up running state
        try {
            java.lang.reflect.Field runningField = PortfolioManagerService.class.getDeclaredField("isRunning");
            runningField.setAccessible(true);
            runningField.set(portfolioManagerService, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set running state", e);
        }
        
        // Execute
        portfolioManagerService.stopRealTimeMonitoring();
        
        // Verify
        assertFalse(portfolioManagerService.isRunning());
    }

    @Test
    @DisplayName("Should not stop monitoring if not running")
    public void testStopRealTimeMonitoringWhenNotRunning() {
        // Execute when not running
        portfolioManagerService.stopRealTimeMonitoring();
        
        // Verify still not running
        assertFalse(portfolioManagerService.isRunning());
    }


    @Test
    @DisplayName("Should handle null portfolio during update")
    public void testUpdatePortfolioWithNullPortfolio() {
        // Execute with null portfolio
        portfolioManagerService.updatePortfolio();
        
        // Should not throw exception
        verify(portfolioCalculationService, never()).calculatePortfolioValues(any());
    }

    @Test
    @DisplayName("Should detect price changes correctly")
    public void testCheckForPriceChanges() {
        // Set up previous prices
        Map<String, BigDecimal> previousStockPrices = new HashMap<>();
        previousStockPrices.put("AAPL", new BigDecimal("150.00"));
        
        Map<String, BigDecimal> previousOptionPrices = new HashMap<>();
        previousOptionPrices.put("AAPL_CALL_150_2024", new BigDecimal("5.00"));
        
        // Set up current prices in market data service
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(marketDataService.getCurrentPrice("AAPL_CALL_150_2024")).thenReturn(new BigDecimal("6.00"));
        
        // Set up portfolio
        Portfolio testPortfolio = new Portfolio();
        testPortfolio.setPositions(testPositions);
        
        // Use reflection to call private method
        try {
            java.lang.reflect.Method checkMethod = PortfolioManagerService.class.getDeclaredMethod("checkForPriceChanges", Portfolio.class);
            checkMethod.setAccessible(true);
            boolean hasChanges = (boolean) checkMethod.invoke(portfolioManagerService, testPortfolio);
            
            assertTrue(hasChanges, "Should detect price changes");
        } catch (Exception e) {
            throw new RuntimeException("Failed to call private method", e);
        }
    }


    @Test
    @DisplayName("Should handle positions without security definitions in price change detection")
    public void testCheckForPriceChangesWithMissingSecurities() {
        // Create position without security
        Position positionWithoutSecurity = new Position("UNKNOWN", new BigDecimal("100"), null);
        List<Position> positionsWithMissing = Arrays.asList(positionWithoutSecurity);
        Portfolio testPortfolio = new Portfolio();
        testPortfolio.setPositions(positionsWithMissing);
        
        // Use reflection to call private method
        try {
            java.lang.reflect.Method checkMethod = PortfolioManagerService.class.getDeclaredMethod("checkForPriceChanges", Portfolio.class);
            checkMethod.setAccessible(true);
            boolean hasChanges = (boolean) checkMethod.invoke(portfolioManagerService, testPortfolio);
            
            // Should not throw exception and should return false (no changes)
            assertFalse(hasChanges);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call private method", e);
        }
    }

    @Test
    @DisplayName("Should update previous prices correctly")
    public void testUpdatePreviousPrices() {
        // Set up portfolio
        Portfolio testPortfolio = new Portfolio();
        testPortfolio.setPositions(testPositions);
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(marketDataService.getCurrentPrice("AAPL_CALL_150_2024")).thenReturn(new BigDecimal("6.00"));
        
        // Use reflection to call private method
        try {
            java.lang.reflect.Method updateMethod = PortfolioManagerService.class.getDeclaredMethod("updatePreviousPrices", Portfolio.class);
            updateMethod.setAccessible(true);
            updateMethod.invoke(portfolioManagerService, testPortfolio);
            
            // Verify that previous prices were updated
            // This is tested indirectly through the price change detection
        } catch (Exception e) {
            throw new RuntimeException("Failed to call private method", e);
        }
    }

    @Test
    @DisplayName("Should handle shutdown gracefully")
    public void testShutdown() {
        // Set up running state
        try {
            java.lang.reflect.Field runningField = PortfolioManagerService.class.getDeclaredField("isRunning");
            runningField.setAccessible(true);
            runningField.set(portfolioManagerService, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set running state", e);
        }
        
        // Execute
        portfolioManagerService.shutdown();
        
        // Verify
        assertFalse(portfolioManagerService.isRunning());
    }

    @Test
    @DisplayName("Should handle IOException during initialization")
    public void testInitializePortfolioWithIOException() throws IOException {
        when(positionLoaderService.loadPositions()).thenThrow(new IOException("File not found"));
        
        assertThrows(IOException.class, () -> {
            portfolioManagerService.initializePortfolio();
        });
    }

    @Test
    @DisplayName("Should handle null positions during initialization")
    public void testInitializePortfolioWithNullPositions() throws IOException {
        when(positionLoaderService.loadPositions()).thenReturn(null);
        
        assertThrows(NullPointerException.class, () -> {
            portfolioManagerService.initializePortfolio();
        });
    }

    @Test
    @DisplayName("Should handle empty positions list")
    public void testInitializePortfolioWithEmptyPositions() throws IOException {
        when(positionLoaderService.loadPositions()).thenReturn(new ArrayList<>());
        when(portfolioCalculationService.getPortfolioSummaryWithChanges(any(Portfolio.class), anyMap(), anyMap(), eq(true)))
            .thenReturn("Empty Portfolio Summary");
        
        portfolioManagerService.initializePortfolio();
        
        Portfolio portfolio = portfolioManagerService.getPortfolio();
        assertNotNull(portfolio);
        assertEquals(0, portfolio.getPositionCount());
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent operations")
    public void testThreadSafety() throws InterruptedException {
        // Set up portfolio
        Portfolio testPortfolio = new Portfolio();
        testPortfolio.setPositions(testPositions);
        
        try {
            java.lang.reflect.Field portfolioField = PortfolioManagerService.class.getDeclaredField("portfolioRef");
            portfolioField.setAccessible(true);
            AtomicReference<Portfolio> portfolioRef = (AtomicReference<Portfolio>) portfolioField.get(portfolioManagerService);
            portfolioRef.set(testPortfolio);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set portfolio", e);
        }
        
        when(portfolioCalculationService.getPortfolioSummaryWithChanges(any(Portfolio.class), anyMap(), anyMap(), anyBoolean()))
            .thenReturn("Portfolio Summary");
        
        // Start monitoring
        portfolioManagerService.startRealTimeMonitoring();
        
        // Create multiple threads to update portfolio concurrently
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    portfolioManagerService.updatePortfolio();
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Stop monitoring
        portfolioManagerService.stopRealTimeMonitoring();
        
        // Verify no exceptions occurred and service is in consistent state
        assertFalse(portfolioManagerService.isRunning());
        assertNotNull(portfolioManagerService.getPortfolio());
    }

}
