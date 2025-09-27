package com.portfolio.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    
    private final Cache<String, Security> securityByTickerCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();
    
    private final Cache<SecurityType, List<Security>> securitiesByTypeCache = CacheBuilder.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .recordStats()
            .build();
    
    private final Cache<String, List<Security>> allSecuritiesCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .recordStats()
            .build();
    
    private final Cache<String, BigDecimal> priceCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats()
            .build();
    
    public Optional<Security> getSecurityByTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Security security = securityByTickerCache.getIfPresent(ticker.trim().toUpperCase());
        return Optional.ofNullable(security);
    }
    
    public void putSecurityByTicker(String ticker, Security security) {
        if (ticker != null && security != null) {
            securityByTickerCache.put(ticker.trim().toUpperCase(), security);
        }
    }
    
    public Optional<List<Security>> getSecuritiesByType(SecurityType type) {
        if (type == null) {
            return Optional.empty();
        }
        
        List<Security> securities = securitiesByTypeCache.getIfPresent(type);
        return Optional.ofNullable(securities);
    }
    
    public void putSecuritiesByType(SecurityType type, List<Security> securities) {
        if (type != null && securities != null) {
            securitiesByTypeCache.put(type, securities);
        }
    }
    
    public Optional<List<Security>> getAllSecurities() {
        List<Security> securities = allSecuritiesCache.getIfPresent("ALL");
        return Optional.ofNullable(securities);
    }
    
    public void putAllSecurities(List<Security> securities) {
        if (securities != null) {
            allSecuritiesCache.put("ALL", securities);
        }
    }
    
    public Optional<BigDecimal> getPrice(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return Optional.empty();
        }
        
        BigDecimal price = priceCache.getIfPresent(ticker.trim().toUpperCase());
        return Optional.ofNullable(price);
    }
    
    public void putPrice(String ticker, BigDecimal price) {
        if (ticker != null && price != null) {
            priceCache.put(ticker.trim().toUpperCase(), price);
        }
    }
    
    public void invalidateSecurityByTicker(String ticker) {
        if (ticker != null) {
            securityByTickerCache.invalidate(ticker.trim().toUpperCase());
        }
    }
    
    public void invalidateSecuritiesByType(SecurityType type) {
        if (type != null) {
            securitiesByTypeCache.invalidate(type);
        }
    }
    
    public void invalidateAllSecurities() {
        allSecuritiesCache.invalidateAll();
    }
    
    public void invalidatePrice(String ticker) {
        if (ticker != null) {
            priceCache.invalidate(ticker.trim().toUpperCase());
        }
    }
    
    public void clearAllCaches() {
        securityByTickerCache.invalidateAll();
        securitiesByTypeCache.invalidateAll();
        allSecuritiesCache.invalidateAll();
        priceCache.invalidateAll();
    }
    
    public CacheStats getCacheStats() {
        return new CacheStats(
            securityByTickerCache.stats(),
            securitiesByTypeCache.stats(),
            allSecuritiesCache.stats(),
            priceCache.stats()
        );
    }
    
    public void logCacheStats() {
        // Logging removed for dependency compliance
    }
    
    public record CacheStats(
        com.google.common.cache.CacheStats securityByTicker,
        com.google.common.cache.CacheStats securitiesByType,
        com.google.common.cache.CacheStats allSecurities,
        com.google.common.cache.CacheStats price
    ) {
        public double securityByTickerHitRate() {
            return securityByTicker.hitRate() * 100;
        }
        
        public double securityByTickerMissRate() {
            return (1.0 - securityByTicker.hitRate()) * 100;
        }
        
        public double securitiesByTypeHitRate() {
            return securitiesByType.hitRate() * 100;
        }
        
        public double securitiesByTypeMissRate() {
            return (1.0 - securitiesByType.hitRate()) * 100;
        }
        
        public double allSecuritiesHitRate() {
            return allSecurities.hitRate() * 100;
        }
        
        public double allSecuritiesMissRate() {
            return (1.0 - allSecurities.hitRate()) * 100;
        }
        
        public double priceHitRate() {
            return price.hitRate() * 100;
        }
        
        public double priceMissRate() {
            return (1.0 - price.hitRate()) * 100;
        }
    }
}
