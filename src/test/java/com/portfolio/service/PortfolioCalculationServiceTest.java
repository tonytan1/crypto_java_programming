package com.portfolio.service;

import com.portfolio.model.*;
import com.portfolio.repository.SecurityRepository;
import com.portfolio.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioCalculationService
 */
public class PortfolioCalculationServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private OptionPricingService optionPricingService;

    @Mock
    private MarketDataService marketDataService;

    private PortfolioCalculationService portfolioCalculationService;
    private Portfolio testPortfolio;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create the service under test
        portfolioCalculationService = new PortfolioCalculationService();
        
        // Use reflection to inject dependencies
        try {
            java.lang.reflect.Field marketDataServiceField = PortfolioCalculationService.class.getDeclaredField("marketDataService");
            marketDataServiceField.setAccessible(true);
            marketDataServiceField.set(portfolioCalculationService, marketDataService);
            
            java.lang.reflect.Field optionPricingServiceField = PortfolioCalculationService.class.getDeclaredField("optionPricingService");
            optionPricingServiceField.setAccessible(true);
            optionPricingServiceField.set(portfolioCalculationService, optionPricingService);
            
            java.lang.reflect.Field eventPublisherField = PortfolioCalculationService.class.getDeclaredField("eventPublisher");
            eventPublisherField.setAccessible(true);
            eventPublisherField.set(portfolioCalculationService, eventPublisher);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        }
        
        // Create test portfolio
        testPortfolio = createTestPortfolio();
        
        // Ensure portfolio is properly initialized
        assertNotNull(testPortfolio);
        assertNotNull(testPortfolio.getPositions());
        assertEquals(3, testPortfolio.getPositions().size());
    }

    private Portfolio createTestPortfolio() {
        Portfolio portfolio = new Portfolio();
        
        // Create test positions
        List<Position> positions = new ArrayList<>();
        
        // Stock position
        Security aaplStock = createStock("AAPL", new BigDecimal("150.00"));
        Position aaplPosition = new Position("AAPL", new BigDecimal("100"), aaplStock);
        positions.add(aaplPosition);
        
        // Call option position
        Security aaplCall = createCallOption("AAPL-CALL-150-2024", new BigDecimal("150.00"));
        Position callPosition = new Position("AAPL-CALL-150-2024", new BigDecimal("10"), aaplCall);
        positions.add(callPosition);
        
        // Put option position
        Security aaplPut = createPutOption("AAPL-PUT-150-2024", new BigDecimal("150.00"));
        Position putPosition = new Position("AAPL-PUT-150-2024", new BigDecimal("5"), aaplPut);
        positions.add(putPosition);
        
        portfolio.setPositions(positions);
        return portfolio;
    }

    private Security createStock(String ticker, BigDecimal initialPrice) {
        Security stock = new Security();
        stock.setTicker(ticker);
        stock.setType(SecurityType.STOCK);
        stock.setMu(new BigDecimal("0.10"));
        stock.setSigma(new BigDecimal("0.25"));
        return stock;
    }

    private Security createCallOption(String ticker, BigDecimal strike) {
        Security call = new Security();
        call.setTicker(ticker);
        call.setType(SecurityType.CALL);
        call.setStrike(strike);
        call.setMaturity(LocalDate.of(2025, 12, 20));
        call.setMu(new BigDecimal("0.10"));
        call.setSigma(new BigDecimal("0.25"));
        return call;
    }

    private Security createPutOption(String ticker, BigDecimal strike) {
        Security put = new Security();
        put.setTicker(ticker);
        put.setType(SecurityType.PUT);
        put.setStrike(strike);
        put.setMaturity(LocalDate.of(2025, 12, 20));
        put.setMu(new BigDecimal("0.10"));
        put.setSigma(new BigDecimal("0.25"));
        return put;
    }

    @AfterEach
    public void tearDown() {
        // Reset all mocks to clear any state
        reset(securityRepository, eventPublisher, marketDataService, optionPricingService);
    }

    @Test
    @DisplayName("Should calculate portfolio values correctly")
    public void testCalculatePortfolioValues() {
        // Mock market data service to return prices
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("160.00"));
        when(marketDataService.getCurrentPrice("AAPL_CALL_150_2024")).thenReturn(new BigDecimal("15.00"));
        when(marketDataService.getCurrentPrice("AAPL_PUT_150_2024")).thenReturn(new BigDecimal("8.00"));
        
        // Calculate portfolio values
        portfolioCalculationService.calculatePortfolioValues(testPortfolio);
        
        // Verify positions have market values set
        for (Position position : testPortfolio.getPositions()) {
            assertNotNull(position.getMarketValue(), "Market value should be set for " + position.getSecurity().getTicker());
            assertTrue(position.getMarketValue().compareTo(BigDecimal.ZERO) >= 0, "Market value should be non-negative");
        }
    }

    @Test
    @DisplayName("Should calculate position value for stock")
    public void testCalculatePositionValueForStock() {
        // Ensure test portfolio is properly set up
        assertNotNull(testPortfolio);
        assertNotNull(testPortfolio.getPositions());
        assertTrue(testPortfolio.getPositions().size() >= 1, "Portfolio should have at least 1 position");
        
        Position stockPosition = testPortfolio.getPositions().get(0); // AAPL stock
        assertNotNull(stockPosition, "Stock position should not be null");
        assertNotNull(stockPosition.getSecurity(), "Stock position security should not be null");
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("160.00"));
        
        portfolioCalculationService.calculatePositionValue(stockPosition);
        
        assertNotNull(stockPosition.getMarketValue());
        // Market value should be stock price * quantity
        assertEquals(new BigDecimal("16000.00"), stockPosition.getMarketValue()); // 160 * 100
    }

    @Test
    @DisplayName("Should calculate position value for call option")
    public void testCalculatePositionValueForCallOption() {
        // Ensure test portfolio is properly set up
        assertNotNull(testPortfolio);
        assertNotNull(testPortfolio.getPositions());
        assertTrue(testPortfolio.getPositions().size() >= 2, "Portfolio should have at least 2 positions");
        
        Position callPosition = testPortfolio.getPositions().get(1); // AAPL call
        assertNotNull(callPosition, "Call position should not be null");
        assertNotNull(callPosition.getSecurity(), "Call position security should not be null");
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        portfolioCalculationService.calculatePositionValue(callPosition);
        
        assertNotNull(callPosition.getMarketValue());
        // Market value should be option price * quantity
        assertEquals(new BigDecimal("80.00"), callPosition.getMarketValue()); // 8 * 10
    }

    @Test
    @DisplayName("Should calculate position value for put option")
    public void testCalculatePositionValueForPutOption() {
        // Ensure test portfolio is properly set up
        assertNotNull(testPortfolio);
        assertNotNull(testPortfolio.getPositions());
        assertTrue(testPortfolio.getPositions().size() >= 3, "Portfolio should have at least 3 positions");
        
        Position putPosition = testPortfolio.getPositions().get(2); // AAPL put
        assertNotNull(putPosition, "Put position should not be null");
        assertNotNull(putPosition.getSecurity(), "Put position security should not be null");
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("5.00"));
        
        portfolioCalculationService.calculatePositionValue(putPosition);
        
        assertNotNull(putPosition.getMarketValue());
        // Market value should be option price * quantity
        assertTrue(putPosition.getMarketValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should handle position without security definition")
    public void testCalculatePositionValueWithoutSecurity() {
        Position positionWithoutSecurity = new Position("UNKNOWN", new BigDecimal("100"), null);
        
        portfolioCalculationService.calculatePositionValue(positionWithoutSecurity);
        
        // Should not throw exception and market value should be zero (not null)
        assertNotNull(positionWithoutSecurity.getMarketValue());
        assertEquals(BigDecimal.ZERO, positionWithoutSecurity.getMarketValue());
    }

    @Test
    @DisplayName("Should update market data and recalculate portfolio")
    public void testUpdateMarketDataAndRecalculate() {
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("160.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        portfolioCalculationService.updateMarketDataAndRecalculate(testPortfolio);
        
        // Verify portfolio was recalculated
        assertNotNull(testPortfolio.getTotalNAV());
        assertNotNull(testPortfolio.getLastUpdated());
    }

    @Test
    @DisplayName("Should generate portfolio summary")
    public void testGetPortfolioSummary() {
        // Mock the market data service
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        // Calculate portfolio values first to set current prices
        portfolioCalculationService.calculatePortfolioValues(testPortfolio);
        
        String summary = portfolioCalculationService.getPortfolioSummary(testPortfolio);
        
        assertNotNull(summary);
        assertTrue(summary.contains("Portfolio Summary"));
        assertTrue(summary.contains("AAPL"));
        assertNotNull(testPortfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should generate portfolio summary with changes")
    public void testGetPortfolioSummaryWithChanges() {
        // Mock the market data service
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        // Calculate portfolio values first to set current prices
        portfolioCalculationService.calculatePortfolioValues(testPortfolio);
        testPortfolio.setLastUpdated(java.time.LocalDateTime.now());
        
        Map<String, BigDecimal> previousStockPrices = new HashMap<>();
        previousStockPrices.put("AAPL", new BigDecimal("150.00"));
        
        Map<String, BigDecimal> previousOptionPrices = new HashMap<>();
        previousOptionPrices.put("AAPL-CALL-150-2024", new BigDecimal("5.00"));
        previousOptionPrices.put("AAPL-PUT-150-2024", new BigDecimal("4.50"));
        
        String summary = portfolioCalculationService.getPortfolioSummaryWithChanges(
            testPortfolio, previousStockPrices, previousOptionPrices, false);
        
        assertNotNull(summary);
        assertTrue(summary.contains("PORTFOLIO UPDATE"));
        assertTrue(summary.contains("Total Positions: 3"));
        assertNotNull(testPortfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should handle initial portfolio summary")
    public void testGetPortfolioSummaryWithChangesInitial() {
        // Mock the market data service
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        // Calculate portfolio values first to set current prices
        portfolioCalculationService.calculatePortfolioValues(testPortfolio);
        testPortfolio.setLastUpdated(java.time.LocalDateTime.now());
        
        Map<String, BigDecimal> previousStockPrices = new HashMap<>();
        Map<String, BigDecimal> previousOptionPrices = new HashMap<>();
        
        String summary = portfolioCalculationService.getPortfolioSummaryWithChanges(
            testPortfolio, previousStockPrices, previousOptionPrices, true);
        
        assertNotNull(summary);
        assertTrue(summary.contains("INITIAL PORTFOLIO SUMMARY"));
        assertTrue(summary.contains("Total Positions: 3"));
        assertNotNull(testPortfolio.getTotalNAV());
        assertTrue(summary.contains("All positions marked as NEW"));
    }

    @Test
    @DisplayName("Should handle null portfolio gracefully")
    public void testCalculatePortfolioValuesWithNullPortfolio() {
        assertThrows(NullPointerException.class, 
            () -> portfolioCalculationService.calculatePortfolioValues(null));
    }

    @Test
    @DisplayName("Should handle empty portfolio")
    public void testCalculatePortfolioValuesWithEmptyPortfolio() {
        Portfolio emptyPortfolio = new Portfolio();
        emptyPortfolio.setPositions(new ArrayList<>());
        
        portfolioCalculationService.calculatePortfolioValues(emptyPortfolio);
        
        assertNotNull(emptyPortfolio.getTotalNAV());
        assertEquals(BigDecimal.ZERO, emptyPortfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    public void testThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        List<Exception> exceptions = new ArrayList<>();
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        portfolioCalculationService.calculatePortfolioValues(testPortfolio);
                        portfolioCalculationService.getPortfolioSummary(testPortfolio);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within timeout");
        executor.shutdown();
        
        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access: " + exceptions);
        
        // Verify portfolio state is consistent
        assertNotNull(testPortfolio.getTotalNAV());
        assertNotNull(testPortfolio.getLastUpdated());
    }

    @Test
    @DisplayName("Should handle short positions correctly")
    public void testShortPositions() {
        // Create a short position (negative quantity)
        Security aaplStock = createStock("AAPL", new BigDecimal("150.00"));
        Position shortPosition = new Position("AAPL", new BigDecimal("-50"), aaplStock);
        
        Portfolio shortPortfolio = new Portfolio();
        shortPortfolio.setPositions(Arrays.asList(shortPosition));
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        
        portfolioCalculationService.calculatePortfolioValues(shortPortfolio);
        
        assertNotNull(shortPortfolio.getTotalNAV());
        // Short position should result in negative market value
        assertTrue(shortPosition.getMarketValue().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    @DisplayName("Should handle expired options correctly")
    public void testExpiredOptions() {
        // Create an expired option
        Security expiredCall = new Security();
        expiredCall.setTicker("AAPL-CALL-150-EXPIRED");
        expiredCall.setType(SecurityType.CALL);
        expiredCall.setStrike(new BigDecimal("150.00"));
        expiredCall.setMaturity(LocalDate.now().minusDays(1));
        expiredCall.setMu(new BigDecimal("0.10"));
        expiredCall.setSigma(new BigDecimal("0.25"));
        
        Position expiredPosition = new Position("AAPL-CALL-150-EXPIRED", new BigDecimal("10"), expiredCall);
        Portfolio expiredPortfolio = new Portfolio();
        expiredPortfolio.setPositions(Arrays.asList(expiredPosition));
        
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(BigDecimal.ZERO); // Expired options return zero
        
        portfolioCalculationService.calculatePortfolioValues(expiredPortfolio);
        
        // Expired option should have zero value
        assertEquals(BigDecimal.ZERO, expiredPosition.getMarketValue());
    }

    @Test
    @DisplayName("Should calculate NAV correctly with mixed positions")
    public void testNAVCalculationWithMixedPositions() {
        when(marketDataService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("155.00"));
        when(optionPricingService.calculateOptionPrice(any(Security.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("8.00"));
        
        portfolioCalculationService.calculatePortfolioValues(testPortfolio);
        
        // Verify NAV was calculated
        assertNotNull(testPortfolio.getTotalNAV());
        assertTrue(testPortfolio.getTotalNAV().compareTo(BigDecimal.ZERO) > 0);
    }
}
