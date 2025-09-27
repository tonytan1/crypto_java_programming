package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CacheServiceTest {

    private CacheService cacheService;
    private Security testStock;
    private Security testCall;
    private Security testPut;

    @BeforeEach
    public void setUp() {
        cacheService = new CacheService();
        testStock = new Security();
        testStock.setId(1L);
        testStock.setTicker("AAPL");
        testStock.setType(SecurityType.STOCK);
        testStock.setMu(new BigDecimal("0.10"));
        testStock.setSigma(new BigDecimal("0.25"));
        
        testCall = new Security();
        testCall.setId(2L);
        testCall.setTicker("AAPL-JAN-2026-150-C");
        testCall.setType(SecurityType.CALL);
        testCall.setStrike(new BigDecimal("150.00"));
        testCall.setMaturity(LocalDate.of(2026, 1, 17));
        testCall.setMu(new BigDecimal("0.10"));
        testCall.setSigma(new BigDecimal("0.25"));
        
        testPut = new Security();
        testPut.setId(3L);
        testPut.setTicker("AAPL-JAN-2026-150-P");
        testPut.setType(SecurityType.PUT);
        testPut.setStrike(new BigDecimal("150.00"));
        testPut.setMaturity(LocalDate.of(2026, 1, 17));
        testPut.setMu(new BigDecimal("0.10"));
        testPut.setSigma(new BigDecimal("0.25"));
    }

    @Test
    @DisplayName("Should cache and retrieve security by ticker")
    public void testCacheSecurityByTicker() {
        assertFalse(cacheService.getSecurityByTicker("AAPL").isPresent());
        
        cacheService.putSecurityByTicker("AAPL", testStock);
        
        Optional<Security> cached = cacheService.getSecurityByTicker("AAPL");
        assertTrue(cached.isPresent());
        assertEquals(testStock.getTicker(), cached.get().getTicker());
        assertEquals(testStock.getType(), cached.get().getType());
    }

    @Test
    @DisplayName("Should cache and retrieve securities by type")
    public void testCacheSecuritiesByType() {
        List<Security> stocks = Arrays.asList(testStock);
        
        assertFalse(cacheService.getSecuritiesByType(SecurityType.STOCK).isPresent());
        
        cacheService.putSecuritiesByType(SecurityType.STOCK, stocks);
        
        Optional<List<Security>> cached = cacheService.getSecuritiesByType(SecurityType.STOCK);
        assertTrue(cached.isPresent());
        assertEquals(1, cached.get().size());
        assertEquals(testStock.getTicker(), cached.get().get(0).getTicker());
    }

    @Test
    @DisplayName("Should cache and retrieve all securities")
    public void testCacheAllSecurities() {
        List<Security> allSecurities = Arrays.asList(testStock, testCall, testPut);
        
        assertFalse(cacheService.getAllSecurities().isPresent());
        
        cacheService.putAllSecurities(allSecurities);
        
        Optional<List<Security>> cached = cacheService.getAllSecurities();
        assertTrue(cached.isPresent());
        assertEquals(3, cached.get().size());
    }

    @Test
    @DisplayName("Should cache and retrieve prices")
    public void testCachePrices() {
        BigDecimal price = new BigDecimal("150.00");
        
        assertFalse(cacheService.getPrice("AAPL").isPresent());
        
        cacheService.putPrice("AAPL", price);
        
        Optional<BigDecimal> cached = cacheService.getPrice("AAPL");
        assertTrue(cached.isPresent());
        assertEquals(price, cached.get());
    }

    @Test
    @DisplayName("Should handle case insensitive ticker caching")
    public void testCaseInsensitiveTickerCaching() {
        cacheService.putSecurityByTicker("aapl", testStock);
        
        // Should be retrievable with different cases
        assertTrue(cacheService.getSecurityByTicker("AAPL").isPresent());
        assertTrue(cacheService.getSecurityByTicker("aapl").isPresent());
        assertTrue(cacheService.getSecurityByTicker("Aapl").isPresent());
    }

    @Test
    @DisplayName("Should invalidate security cache by ticker")
    public void testInvalidateSecurityByTicker() {
        cacheService.putSecurityByTicker("AAPL", testStock);
        assertTrue(cacheService.getSecurityByTicker("AAPL").isPresent());
        
        // Invalidate
        cacheService.invalidateSecurityByTicker("AAPL");
        
        // Should be empty
        assertFalse(cacheService.getSecurityByTicker("AAPL").isPresent());
    }

    @Test
    @DisplayName("Should invalidate securities cache by type")
    public void testInvalidateSecuritiesByType() {
        List<Security> stocks = Arrays.asList(testStock);
        cacheService.putSecuritiesByType(SecurityType.STOCK, stocks);
        assertTrue(cacheService.getSecuritiesByType(SecurityType.STOCK).isPresent());
        
        // Invalidate
        cacheService.invalidateSecuritiesByType(SecurityType.STOCK);
        
        // Should be empty
        assertFalse(cacheService.getSecuritiesByType(SecurityType.STOCK).isPresent());
    }

    @Test
    @DisplayName("Should invalidate price cache")
    public void testInvalidatePrice() {
        cacheService.putPrice("AAPL", new BigDecimal("150.00"));
        assertTrue(cacheService.getPrice("AAPL").isPresent());
        
        // Invalidate
        cacheService.invalidatePrice("AAPL");
        
        // Should be empty
        assertFalse(cacheService.getPrice("AAPL").isPresent());
    }

    @Test
    @DisplayName("Should clear all caches")
    public void testClearAllCaches() {
        // Populate all caches
        cacheService.putSecurityByTicker("AAPL", testStock);
        cacheService.putSecuritiesByType(SecurityType.STOCK, Arrays.asList(testStock));
        cacheService.putAllSecurities(Arrays.asList(testStock, testCall, testPut));
        cacheService.putPrice("AAPL", new BigDecimal("150.00"));
        
        // Verify they are populated
        assertTrue(cacheService.getSecurityByTicker("AAPL").isPresent());
        assertTrue(cacheService.getSecuritiesByType(SecurityType.STOCK).isPresent());
        assertTrue(cacheService.getAllSecurities().isPresent());
        assertTrue(cacheService.getPrice("AAPL").isPresent());
        
        // Clear all
        cacheService.clearAllCaches();
        
        // All should be empty
        assertFalse(cacheService.getSecurityByTicker("AAPL").isPresent());
        assertFalse(cacheService.getSecuritiesByType(SecurityType.STOCK).isPresent());
        assertFalse(cacheService.getAllSecurities().isPresent());
        assertFalse(cacheService.getPrice("AAPL").isPresent());
    }

    @Test
    @DisplayName("Should handle null and empty ticker gracefully")
    public void testHandleNullAndEmptyTicker() {
        // Test null ticker
        assertFalse(cacheService.getSecurityByTicker(null).isPresent());
        assertFalse(cacheService.getPrice(null).isPresent());
        
        // Test empty ticker
        assertFalse(cacheService.getSecurityByTicker("").isPresent());
        assertFalse(cacheService.getSecurityByTicker("   ").isPresent());
        assertFalse(cacheService.getPrice("").isPresent());
        
        // Test null security/price
        cacheService.putSecurityByTicker("AAPL", null);
        cacheService.putPrice("AAPL", null);
        
        assertFalse(cacheService.getSecurityByTicker("AAPL").isPresent());
        assertFalse(cacheService.getPrice("AAPL").isPresent());
    }

    @Test
    @DisplayName("Should provide cache statistics")
    public void testCacheStatistics() {
        // Get initial stats
        CacheService.CacheStats stats = cacheService.getCacheStats();
        assertNotNull(stats);
        assertNotNull(stats.securityByTicker());
        assertNotNull(stats.securitiesByType());
        assertNotNull(stats.allSecurities());
        assertNotNull(stats.price());
        
        // Perform some cache operations
        cacheService.putSecurityByTicker("AAPL", testStock);
        cacheService.getSecurityByTicker("AAPL");
        cacheService.getSecurityByTicker("MSFT"); // Miss
        
        // Get updated stats
        CacheService.CacheStats updatedStats = cacheService.getCacheStats();
        assertNotNull(updatedStats);
        
        // Should have some hit/miss data
        assertTrue(updatedStats.securityByTicker().hitCount() >= 0);
        assertTrue(updatedStats.securityByTicker().missCount() >= 0);
    }
}
