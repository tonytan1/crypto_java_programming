package com.portfolio.repository;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Cached wrapper for SecurityRepository that provides caching functionality
 * using Google Guava Cache to improve performance for frequently accessed data.
 */
@Repository
public class CachedSecurityRepository {
    
    @Autowired
    private SecurityRepository securityRepository;
    
    @Autowired
    private CacheService cacheService;
    
    public Optional<Security> findByTicker(String ticker) {
        Optional<Security> cachedSecurity = cacheService.getSecurityByTicker(ticker);
        if (cachedSecurity.isPresent()) {
            return cachedSecurity;
        }
        
        Optional<Security> security = securityRepository.findByTicker(ticker);
        
        if (security.isPresent()) {
            cacheService.putSecurityByTicker(ticker, security.get());
        }
        
        return security;
    }
    
    public List<Security> findByType(SecurityType type) {
        Optional<List<Security>> cachedSecurities = cacheService.getSecuritiesByType(type);
        if (cachedSecurities.isPresent()) {
            return cachedSecurities.get();
        }
        
        List<Security> securities = securityRepository.findByType(type);
        
        cacheService.putSecuritiesByType(type, securities);
        
        return securities;
    }
    
    public List<Security> findAll() {
        Optional<List<Security>> cachedSecurities = cacheService.getAllSecurities();
        if (cachedSecurities.isPresent()) {
            return cachedSecurities.get();
        }
        
        List<Security> securities = securityRepository.findAll();
        
        cacheService.putAllSecurities(securities);
        
        return securities;
    }
    
    public Security save(Security security) {
        Security savedSecurity = securityRepository.save(security);
        
        cacheService.invalidateSecurityByTicker(savedSecurity.getTicker());
        cacheService.invalidateSecuritiesByType(savedSecurity.getType());
        cacheService.invalidateAllSecurities();
        
        return savedSecurity;
    }
    
    public void deleteById(Long id) {
        cacheService.clearAllCaches();
        securityRepository.deleteById(id);
    }
    
    public void deleteAll() {
        securityRepository.deleteAll();
        cacheService.clearAllCaches();
    }
    
    public boolean existsByTicker(String ticker) {
        return findByTicker(ticker).isPresent();
    }
    
    public long countByType(SecurityType type) {
        return findByType(type).size();
    }
    
    public long count() {
        return findAll().size();
    }
    
    public Optional<Security> refreshByTicker(String ticker) {
        cacheService.invalidateSecurityByTicker(ticker);
        return findByTicker(ticker);
    }
    
    public List<Security> refreshByType(SecurityType type) {
        cacheService.invalidateSecuritiesByType(type);
        return findByType(type);
    }
    
    public List<Security> refreshAll() {
        cacheService.invalidateAllSecurities();
        return findAll();
    }
}
