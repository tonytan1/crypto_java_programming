package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for initializing sample data in the database.
 */
@Service
public class DataInitializationService {
    
    private static final Logger logger = Logger.getLogger(DataInitializationService.class.getName());
    
    @Autowired
    private SecurityRepository securityRepository;
    
    public void initializeSampleData() {
        logger.info("=== DATA INITIALIZATION SERVICE STARTING ===");
        logger.info("Initializing sample data...");
        
        try {
            // Check if data already exists
            List<Security> existingSecurities = securityRepository.findAll();
            logger.info("Found " + existingSecurities.size() + " existing securities in database");
            if (!existingSecurities.isEmpty()) {
                logger.info("Sample data exists, resetting");
                securityRepository.deleteAll();
            }
        } catch (Exception e) {
            logger.severe("Error checking existing securities: " + e.getMessage());
            return;
        }
        
        // Create sample stocks
        Security aaplStock = new Security("AAPL", SecurityType.STOCK, 
                new BigDecimal("0.08"), new BigDecimal("0.25"));
        Security telsaStock = new Security("TELSA", SecurityType.STOCK, 
                new BigDecimal("0.12"), new BigDecimal("0.35"));
        
        // Create sample options (mix of expired and active)
        // Expired options (from 2020) - will show $0.00
        Security aaplCallExpired = new Security("AAPL-OCT-2020-110-C", SecurityType.CALL, 
                new BigDecimal("110.00"), LocalDate.of(2020, 10, 16), 
                new BigDecimal("0.08"), new BigDecimal("0.25"));
        Security aaplPutExpired = new Security("AAPL-OCT-2020-110-P", SecurityType.PUT, 
                new BigDecimal("110.00"), LocalDate.of(2020, 10, 16), 
                new BigDecimal("0.08"), new BigDecimal("0.25"));
        Security telsaCallExpired = new Security("TELSA-NOV-2020-400-C", SecurityType.CALL, 
                new BigDecimal("400.00"), LocalDate.of(2020, 11, 20), 
                new BigDecimal("0.12"), new BigDecimal("0.35"));
        Security telsaPutExpired = new Security("TELSA-DEC-2020-400-P", SecurityType.PUT, 
                new BigDecimal("400.00"), LocalDate.of(2020, 12, 18), 
                new BigDecimal("0.12"), new BigDecimal("0.35"));
        
        // Active options (future dates) - will show real Black-Scholes prices
        Security aaplCallActive = new Security("AAPL-JAN-2026-150-C", SecurityType.CALL, 
                new BigDecimal("150.00"), LocalDate.of(2026, 1, 17), 
                new BigDecimal("0.08"), new BigDecimal("0.25"));
        Security aaplPutActive = new Security("AAPL-JAN-2026-150-P", SecurityType.PUT, 
                new BigDecimal("150.00"), LocalDate.of(2026, 1, 17), 
                new BigDecimal("0.08"), new BigDecimal("0.25"));
        Security telsaCallActive = new Security("TELSA-FEB-2026-800-C", SecurityType.CALL, 
                new BigDecimal("800.00"), LocalDate.of(2026, 2, 21), 
                new BigDecimal("0.12"), new BigDecimal("0.35"));
        Security telsaPutActive = new Security("TELSA-FEB-2026-800-P", SecurityType.PUT, 
                new BigDecimal("800.00"), LocalDate.of(2026, 2, 21), 
                new BigDecimal("0.12"), new BigDecimal("0.35"));
        
        try {
            // Save all securities
            logger.info("Saving stocks...");
            securityRepository.save(aaplStock);
            securityRepository.save(telsaStock);
            
            // Save expired options
            logger.info("Saving expired options...");
            securityRepository.save(aaplCallExpired);
            securityRepository.save(aaplPutExpired);
            securityRepository.save(telsaCallExpired);
            securityRepository.save(telsaPutExpired);
            
            // Save active options
            logger.info("Saving active options...");
            securityRepository.save(aaplCallActive);
            securityRepository.save(aaplPutActive);
            securityRepository.save(telsaCallActive);
            securityRepository.save(telsaPutActive);
            
            // Verify data was saved
            List<Security> savedSecurities = securityRepository.findAll();
            logger.info("Sample data initialization completed. Created " + savedSecurities.size() + " securities (2 stocks, 4 expired options, 4 active options)");
            logger.info("=== DATA INITIALIZATION SERVICE COMPLETED ===");
            
        } catch (Exception e) {
            logger.severe("Error saving securities: " + e.getMessage());
        }
    }
}
