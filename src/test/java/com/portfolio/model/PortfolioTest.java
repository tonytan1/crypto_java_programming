package com.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Portfolio model
 */
public class PortfolioTest {

    private Portfolio portfolio;
    private List<Position> testPositions;

    @BeforeEach
    public void setUp() {
        portfolio = new Portfolio();
        testPositions = createTestPositions();
    }

    private List<Position> createTestPositions() {
        List<Position> positions = new ArrayList<>();
        
        // Create test securities
        Security aaplStock = createStock("AAPL", new BigDecimal("150.00"));
        Security aaplCall = createCallOption("AAPL_CALL_150_2024", new BigDecimal("150.00"));
        Security aaplPut = createPutOption("AAPL_PUT_150_2024", new BigDecimal("150.00"));
        
        // Create test positions
        positions.add(new Position("AAPL", new BigDecimal("100"), aaplStock));
        positions.add(new Position("AAPL_CALL_150_2024", new BigDecimal("10"), aaplCall));
        positions.add(new Position("AAPL_PUT_150_2024", new BigDecimal("5"), aaplPut));
        
        return positions;
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
        call.setMaturity(LocalDate.of(2024, 12, 20));
        call.setMu(new BigDecimal("0.10"));
        call.setSigma(new BigDecimal("0.25"));
        return call;
    }

    private Security createPutOption(String ticker, BigDecimal strike) {
        Security put = new Security();
        put.setTicker(ticker);
        put.setType(SecurityType.PUT);
        put.setStrike(strike);
        put.setMaturity(LocalDate.of(2024, 12, 20));
        put.setMu(new BigDecimal("0.10"));
        put.setSigma(new BigDecimal("0.25"));
        return put;
    }

    @Test
    @DisplayName("Should create empty portfolio by default")
    public void testCreateEmptyPortfolio() {
        assertNotNull(portfolio);
        assertNotNull(portfolio.getPositions());
        assertTrue(portfolio.getPositions().isEmpty());
        assertEquals(BigDecimal.ZERO, portfolio.getTotalNAV());
        assertNull(portfolio.getLastUpdated());
        assertEquals(0, portfolio.getPositionCount());
    }

    @Test
    @DisplayName("Should set and get positions")
    public void testSetAndGetPositions() {
        portfolio.setPositions(testPositions);
        
        assertNotNull(portfolio.getPositions());
        assertEquals(3, portfolio.getPositions().size());
        assertEquals(testPositions, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should calculate position count correctly")
    public void testGetPositionCount() {
        portfolio.setPositions(testPositions);
        assertEquals(3, portfolio.getPositionCount());
    }

    @Test
    @DisplayName("Should return zero position count for null positions")
    public void testGetPositionCountWithNullPositions() {
        portfolio.setPositions(null);
        assertEquals(0, portfolio.getPositionCount());
    }

    @Test
    @DisplayName("Should return zero position count for empty positions")
    public void testGetPositionCountWithEmptyPositions() {
        portfolio.setPositions(new ArrayList<>());
        assertEquals(0, portfolio.getPositionCount());
    }

    @Test
    @DisplayName("Should set and get total NAV")
    public void testSetAndGetTotalNAV() {
        BigDecimal nav = new BigDecimal("15000.00");
        portfolio.setTotalNAV(nav);
        
        assertEquals(nav, portfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should set and get last updated timestamp")
    public void testSetAndGetLastUpdated() {
        LocalDateTime timestamp = LocalDateTime.now();
        portfolio.setLastUpdated(timestamp);
        
        assertEquals(timestamp, portfolio.getLastUpdated());
    }

    @Test
    @DisplayName("Should handle null positions gracefully")
    public void testHandleNullPositions() {
        portfolio.setPositions(null);
        
        assertEquals(0, portfolio.getPositionCount());
        assertNotNull(portfolio.getPositions());
        assertTrue(portfolio.getPositions().isEmpty());
        assertEquals(BigDecimal.ZERO, portfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should handle empty positions list")
    public void testHandleEmptyPositions() {
        portfolio.setPositions(new ArrayList<>());
        
        assertEquals(0, portfolio.getPositionCount());
        assertNotNull(portfolio.getPositions());
        assertTrue(portfolio.getPositions().isEmpty());
    }

    @Test
    @DisplayName("Should handle large number of positions")
    public void testHandleLargeNumberOfPositions() {
        List<Position> largePositionList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Security stock = createStock("STOCK" + i, new BigDecimal("100.00"));
            largePositionList.add(new Position("STOCK" + i, new BigDecimal("10"), stock));
        }
        
        portfolio.setPositions(largePositionList);
        
        assertEquals(1000, portfolio.getPositionCount());
        assertEquals(largePositionList, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should handle zero NAV")
    public void testHandleZeroNAV() {
        portfolio.setTotalNAV(BigDecimal.ZERO);
        
        assertEquals(BigDecimal.ZERO, portfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should handle negative NAV")
    public void testHandleNegativeNAV() {
        BigDecimal negativeNAV = new BigDecimal("-1000.00");
        portfolio.setTotalNAV(negativeNAV);
        
        assertEquals(negativeNAV, portfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should handle very large NAV values")
    public void testHandleLargeNAV() {
        BigDecimal largeNAV = new BigDecimal("999999999.99");
        portfolio.setTotalNAV(largeNAV);
        
        assertEquals(largeNAV, portfolio.getTotalNAV());
    }

    @Test
    @DisplayName("Should maintain position order")
    public void testMaintainPositionOrder() {
        portfolio.setPositions(testPositions);
        
        List<Position> retrievedPositions = portfolio.getPositions();
        for (int i = 0; i < testPositions.size(); i++) {
            assertEquals(testPositions.get(i), retrievedPositions.get(i));
        }
    }

    @Test
    @DisplayName("Should handle duplicate positions")
    public void testHandleDuplicatePositions() {
        List<Position> duplicatePositions = new ArrayList<>();
        Security aaplStock = createStock("AAPL", new BigDecimal("150.00"));
        
        // Add same position twice
        duplicatePositions.add(new Position("AAPL", new BigDecimal("100"), aaplStock));
        duplicatePositions.add(new Position("AAPL", new BigDecimal("50"), aaplStock));
        
        portfolio.setPositions(duplicatePositions);
        
        assertEquals(2, portfolio.getPositionCount());
        assertEquals(duplicatePositions, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should handle positions with null security")
    public void testHandlePositionsWithNullSecurity() {
        List<Position> positionsWithNull = new ArrayList<>();
        positionsWithNull.add(new Position("UNKNOWN", new BigDecimal("100"), null));
        positionsWithNull.add(new Position("AAPL", new BigDecimal("50"), createStock("AAPL", new BigDecimal("150.00"))));
        
        portfolio.setPositions(positionsWithNull);
        
        assertEquals(2, portfolio.getPositionCount());
        assertEquals(positionsWithNull, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should handle positions with zero size")
    public void testHandlePositionsWithZeroSize() {
        List<Position> positionsWithZero = new ArrayList<>();
        positionsWithZero.add(new Position("AAPL", BigDecimal.ZERO, createStock("AAPL", new BigDecimal("150.00"))));
        positionsWithZero.add(new Position("TSLA", new BigDecimal("100"), createStock("TSLA", new BigDecimal("200.00"))));
        
        portfolio.setPositions(positionsWithZero);
        
        assertEquals(2, portfolio.getPositionCount());
        assertEquals(positionsWithZero, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should handle positions with negative size")
    public void testHandlePositionsWithNegativeSize() {
        List<Position> positionsWithNegative = new ArrayList<>();
        positionsWithNegative.add(new Position("AAPL", new BigDecimal("-100"), createStock("AAPL", new BigDecimal("150.00"))));
        positionsWithNegative.add(new Position("TSLA", new BigDecimal("50"), createStock("TSLA", new BigDecimal("200.00"))));
        
        portfolio.setPositions(positionsWithNegative);
        
        assertEquals(2, portfolio.getPositionCount());
        assertEquals(positionsWithNegative, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    public void testThreadSafety() throws InterruptedException {
        portfolio.setPositions(new ArrayList<>());
        
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    // Read operations
                    int count = portfolio.getPositionCount();
                    List<Position> positions = portfolio.getPositions();
                    
                    // Write operations
                    if (j % 10 == 0) {
                        List<Position> newPositions = new ArrayList<>();
                        if (positions != null) {
                            newPositions.addAll(positions);
                        }
                        Security stock = createStock("STOCK" + threadId + "_" + j, new BigDecimal("100.00"));
                        newPositions.add(new Position("STOCK" + threadId + "_" + j, new BigDecimal("10"), stock));
                        portfolio.setPositions(newPositions);
                    }
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify portfolio is in consistent state
        assertNotNull(portfolio.getPositions());
        assertTrue(portfolio.getPositionCount() >= 0);
    }

    @Test
    @DisplayName("Should handle mixed position types")
    public void testHandleMixedPositionTypes() {
        List<Position> mixedPositions = new ArrayList<>();
        
        // Stock position
        mixedPositions.add(new Position("AAPL", new BigDecimal("100"), createStock("AAPL", new BigDecimal("150.00"))));
        
        // Call option position
        mixedPositions.add(new Position("AAPL_CALL_150_2024", new BigDecimal("10"), createCallOption("AAPL_CALL_150_2024", new BigDecimal("150.00"))));
        
        // Put option position
        mixedPositions.add(new Position("AAPL_PUT_150_2024", new BigDecimal("5"), createPutOption("AAPL_PUT_150_2024", new BigDecimal("150.00"))));
        
        portfolio.setPositions(mixedPositions);
        
        assertEquals(3, portfolio.getPositionCount());
        assertEquals(mixedPositions, portfolio.getPositions());
    }

    @Test
    @DisplayName("Should handle very long ticker names")
    public void testHandleLongTickerNames() {
        List<Position> positionsWithLongTickers = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("A");
        }
        String longTicker = sb.toString(); // 100 character ticker
        Security stock = createStock(longTicker, new BigDecimal("100.00"));
        positionsWithLongTickers.add(new Position(longTicker, new BigDecimal("10"), stock));
        
        portfolio.setPositions(positionsWithLongTickers);
        
        assertEquals(1, portfolio.getPositionCount());
        assertEquals(longTicker, portfolio.getPositions().get(0).getSymbol());
    }

    @Test
    @DisplayName("Should handle positions with very large sizes")
    public void testHandlePositionsWithLargeSizes() {
        List<Position> positionsWithLargeSizes = new ArrayList<>();
        BigDecimal largeSize = new BigDecimal("999999999");
        Security stock = createStock("AAPL", new BigDecimal("150.00"));
        positionsWithLargeSizes.add(new Position("AAPL", largeSize, stock));
        
        portfolio.setPositions(positionsWithLargeSizes);
        
        assertEquals(1, portfolio.getPositionCount());
        assertEquals(largeSize, portfolio.getPositions().get(0).getPositionSize());
    }
}
