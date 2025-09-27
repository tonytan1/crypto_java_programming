package com.portfolio.repository;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Security repository operations.
 */
public interface ISecurityRepository {
    
    List<Security> findAll();
    
    Optional<Security> findByTicker(String ticker);
    
    List<Security> findByType(SecurityType type);
    
    Security save(Security security);
    
    void deleteById(Long id);
    
    void deleteAll();
}

