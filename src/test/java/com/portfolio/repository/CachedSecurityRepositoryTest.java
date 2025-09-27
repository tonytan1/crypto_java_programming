package com.portfolio.repository;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CachedSecurityRepository functionality
 */
public class CachedSecurityRepositoryTest {

    private SecurityRepository securityRepository;
    private CacheService cacheService;
    private CachedSecurityRepository cachedSecurityRepository;
    private Security testStock;
    private Security testCall;

    @BeforeEach
    public void setUp() {
        securityRepository = new MockSecurityRepository();
        cacheService = new MockCacheService();
        cachedSecurityRepository = new CachedSecurityRepository();
        
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field repositoryField = CachedSecurityRepository.class.getDeclaredField("securityRepository");
            repositoryField.setAccessible(true);
            repositoryField.set(cachedSecurityRepository, securityRepository);
            
            java.lang.reflect.Field cacheField = CachedSecurityRepository.class.getDeclaredField("cacheService");
            cacheField.setAccessible(true);
            cacheField.set(cachedSecurityRepository, cacheService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
        
        // Create test securities
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
    }

    @Test
    @DisplayName("Should return cached security when available")
    public void testFindByTickerCacheHit() {
        MockCacheService mockCache = (MockCacheService) cacheService;
        
        // Setup cache hit
        mockCache.setGetSecurityByTickerResult(Optional.of(testStock));
        
        // Call method
        Optional<Security> result = cachedSecurityRepository.findByTicker("AAPL");
        
        // Verify result
        assertTrue(result.isPresent());
        assertEquals(testStock.getTicker(), result.get().getTicker());
    }

    @Test
    @DisplayName("Should fetch from database and cache when cache miss")
    public void testFindByTickerCacheMiss() {
        MockSecurityRepository mockRepo = (MockSecurityRepository) securityRepository;
        MockCacheService mockCache = (MockCacheService) cacheService;
        
        // Setup cache miss and database result
        mockCache.setGetSecurityByTickerResult(Optional.empty());
        mockRepo.setFindByTickerResult(Optional.of(testStock));
        
        // Call method
        Optional<Security> result = cachedSecurityRepository.findByTicker("AAPL");
        
        // Verify result was cached
        assertTrue(mockCache.wasPutSecurityByTickerCalled());
        
        // Verify result
        assertTrue(result.isPresent());
        assertEquals(testStock.getTicker(), result.get().getTicker());
    }

    @Test
    @DisplayName("Should save security and invalidate caches")
    public void testSaveSecurityAndInvalidateCaches() {
        MockSecurityRepository mockRepo = (MockSecurityRepository) securityRepository;
        MockCacheService mockCache = (MockCacheService) cacheService;
        
        // Setup save result
        mockRepo.setSaveResult(testStock);
        
        // Call method
        Security result = cachedSecurityRepository.save(testStock);
        
        // Verify caches were invalidated
        assertTrue(mockCache.wasInvalidateSecurityByTickerCalled());
        assertTrue(mockCache.wasInvalidateSecuritiesByTypeCalled());
        assertTrue(mockCache.wasInvalidateAllSecuritiesCalled());
        
        // Verify result
        assertEquals(testStock, result);
    }

    @Test
    @DisplayName("Should delete all securities and clear all caches")
    public void testDeleteAllSecuritiesAndClearCaches() {
        MockCacheService mockCache = (MockCacheService) cacheService;
        
        // Call method
        cachedSecurityRepository.deleteAll();
        
        // Verify all caches were cleared
        assertTrue(mockCache.wasClearAllCachesCalled());
    }
    
    // Mock implementations
    private static class MockSecurityRepository extends SecurityRepository {
        private Optional<Security> findByTickerResult = Optional.empty();
        private List<Security> findByTypeResult = Arrays.asList();
        private List<Security> findAllResult = Arrays.asList();
        private Security saveResult = null;
        
        public void setFindByTickerResult(Optional<Security> result) {
            this.findByTickerResult = result;
        }
        
        
        public void setSaveResult(Security result) {
            this.saveResult = result;
        }
        
        @Override
        public Optional<Security> findByTicker(String ticker) {
            return findByTickerResult;
        }
        
        @Override
        public List<Security> findByType(SecurityType type) {
            return findByTypeResult;
        }
        
        @Override
        public List<Security> findAll() {
            return findAllResult;
        }
        
        @Override
        public Security save(Security security) {
            return saveResult != null ? saveResult : security;
        }
        
        @Override
        public void deleteById(Long id) {
            // Mock implementation
        }
        
        @Override
        public void deleteAll() {
            // Mock implementation
        }
    }
    
    private static class MockCacheService extends CacheService {
        private Optional<Security> getSecurityByTickerResult = Optional.empty();
        private Optional<List<Security>> getSecuritiesByTypeResult = Optional.empty();
        private Optional<List<Security>> getAllSecuritiesResult = Optional.empty();
        private boolean putSecurityByTickerCalled = false;
        private boolean invalidateSecurityByTickerCalled = false;
        private boolean invalidateSecuritiesByTypeCalled = false;
        private boolean invalidateAllSecuritiesCalled = false;
        private boolean clearAllCachesCalled = false;
        
        public void setGetSecurityByTickerResult(Optional<Security> result) {
            this.getSecurityByTickerResult = result;
        }
        
        
        public boolean wasPutSecurityByTickerCalled() { return putSecurityByTickerCalled; }
        public boolean wasInvalidateSecurityByTickerCalled() { return invalidateSecurityByTickerCalled; }
        public boolean wasInvalidateSecuritiesByTypeCalled() { return invalidateSecuritiesByTypeCalled; }
        public boolean wasInvalidateAllSecuritiesCalled() { return invalidateAllSecuritiesCalled; }
        public boolean wasClearAllCachesCalled() { return clearAllCachesCalled; }
        
        @Override
        public Optional<Security> getSecurityByTicker(String ticker) {
            return getSecurityByTickerResult;
        }
        
        @Override
        public Optional<List<Security>> getSecuritiesByType(SecurityType type) {
            return getSecuritiesByTypeResult;
        }
        
        @Override
        public Optional<List<Security>> getAllSecurities() {
            return getAllSecuritiesResult;
        }
        
        @Override
        public void putSecurityByTicker(String ticker, Security security) {
            putSecurityByTickerCalled = true;
        }
        
        @Override
        public void putSecuritiesByType(SecurityType type, List<Security> securities) {
            // Mock implementation
        }
        
        @Override
        public void putAllSecurities(List<Security> securities) {
            // Mock implementation
        }
        
        @Override
        public void invalidateSecurityByTicker(String ticker) {
            invalidateSecurityByTickerCalled = true;
        }
        
        @Override
        public void invalidateSecuritiesByType(SecurityType type) {
            invalidateSecuritiesByTypeCalled = true;
        }
        
        @Override
        public void invalidateAllSecurities() {
            invalidateAllSecuritiesCalled = true;
        }
        
        @Override
        public void clearAllCaches() {
            clearAllCachesCalled = true;
        }
    }
}