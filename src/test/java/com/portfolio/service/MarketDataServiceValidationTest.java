package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import com.portfolio.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the new strict validation behavior for stock price configuration
 */
public class MarketDataServiceValidationTest {

    @Mock
    private SecurityRepository securityRepository;
    
    @Mock
    private EventPublisher eventPublisher;

    private MarketDataService marketDataService;

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
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
    }

    @Test
    @DisplayName("Should skip stock when price is not configured")
    public void testSkipWhenStockPriceNotConfigured() throws Exception {
        // Create a stock that's not in the configuration
        Security unconfiguredStock = new Security();
        unconfiguredStock.setTicker("UNKNOWN_STOCK");
        unconfiguredStock.setType(SecurityType.STOCK);
        
        List<Security> stocks = Arrays.asList(unconfiguredStock);
        when(securityRepository.findByType(SecurityType.STOCK)).thenReturn(stocks);
        
        // Set empty configuration
        java.lang.reflect.Field initialPricesConfigField = MarketDataService.class.getDeclaredField("initialPricesConfig");
        initialPricesConfigField.setAccessible(true);
        initialPricesConfigField.set(marketDataService, new HashMap<String, String>());
        
        // Should throw exception because NO stocks could be initialized
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            marketDataService.initializePrices();
        });
        
        assertTrue(exception.getMessage().contains("No stocks could be initialized"));
    }

    @Test
    @DisplayName("Should skip stock when price is invalid (non-numeric)")
    public void testSkipWhenStockPriceIsInvalid() throws Exception {
        Security stock = new Security();
        stock.setTicker("INVALID_PRICE_STOCK");
        stock.setType(SecurityType.STOCK);
        
        List<Security> stocks = Arrays.asList(stock);
        when(securityRepository.findByType(SecurityType.STOCK)).thenReturn(stocks);
        
        // Set invalid price configuration
        Map<String, String> invalidPrices = new HashMap<>();
        invalidPrices.put("INVALID_PRICE_STOCK", "not-a-number");
        
        java.lang.reflect.Field initialPricesConfigField = MarketDataService.class.getDeclaredField("initialPricesConfig");
        initialPricesConfigField.setAccessible(true);
        initialPricesConfigField.set(marketDataService, invalidPrices);
        
        // Should throw exception because NO stocks could be initialized (all were skipped)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            marketDataService.initializePrices();
        });
        
        assertTrue(exception.getMessage().contains("No stocks could be initialized"));
    }

    @Test
    @DisplayName("Should skip stock when price is zero or negative")
    public void testSkipWhenStockPriceIsZeroOrNegative() throws Exception {
        Security stock = new Security();
        stock.setTicker("NEGATIVE_PRICE_STOCK");
        stock.setType(SecurityType.STOCK);
        
        List<Security> stocks = Arrays.asList(stock);
        when(securityRepository.findByType(SecurityType.STOCK)).thenReturn(stocks);
        
        // Set negative price configuration
        Map<String, String> negativePrices = new HashMap<>();
        negativePrices.put("NEGATIVE_PRICE_STOCK", "-100.00");
        
        java.lang.reflect.Field initialPricesConfigField = MarketDataService.class.getDeclaredField("initialPricesConfig");
        initialPricesConfigField.setAccessible(true);
        initialPricesConfigField.set(marketDataService, negativePrices);
        
        // Should throw exception because NO stocks could be initialized (all were skipped)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            marketDataService.initializePrices();
        });
        
        assertTrue(exception.getMessage().contains("No stocks could be initialized"));
    }

    @Test
    @DisplayName("Should succeed when all stocks have valid price configurations")
    public void testSuccessWhenAllStocksConfigured() throws Exception {
        Security stock = new Security();
        stock.setTicker("VALID_STOCK");
        stock.setType(SecurityType.STOCK);
        
        List<Security> stocks = Arrays.asList(stock);
        when(securityRepository.findByType(SecurityType.STOCK)).thenReturn(stocks);
        
        // Set valid price configuration
        Map<String, String> validPrices = new HashMap<>();
        validPrices.put("VALID_STOCK", "150.00");
        
        java.lang.reflect.Field initialPricesConfigField = MarketDataService.class.getDeclaredField("initialPricesConfig");
        initialPricesConfigField.setAccessible(true);
        initialPricesConfigField.set(marketDataService, validPrices);
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            marketDataService.initializePrices();
        });
    }

    @Test
    @DisplayName("Should initialize valid stocks and skip invalid ones")
    public void testMixedValidAndInvalidStocks() throws Exception {
        Security validStock = new Security();
        validStock.setTicker("VALID_STOCK");
        validStock.setType(SecurityType.STOCK);
        
        Security invalidStock = new Security();
        invalidStock.setTicker("INVALID_STOCK");
        invalidStock.setType(SecurityType.STOCK);
        
        Security unconfiguredStock = new Security();
        unconfiguredStock.setTicker("UNCONFIGURED_STOCK");
        unconfiguredStock.setType(SecurityType.STOCK);
        
        List<Security> stocks = Arrays.asList(validStock, invalidStock, unconfiguredStock);
        when(securityRepository.findByType(SecurityType.STOCK)).thenReturn(stocks);
        
        // Set mixed configuration - one valid, one invalid, one missing
        Map<String, String> mixedPrices = new HashMap<>();
        mixedPrices.put("VALID_STOCK", "150.00");
        mixedPrices.put("INVALID_STOCK", "not-a-number");
        // UNCONFIGURED_STOCK is intentionally missing
        
        java.lang.reflect.Field initialPricesConfigField = MarketDataService.class.getDeclaredField("initialPricesConfig");
        initialPricesConfigField.setAccessible(true);
        initialPricesConfigField.set(marketDataService, mixedPrices);
        
        // Should succeed because at least one stock (VALID_STOCK) can be initialized
        assertDoesNotThrow(() -> {
            marketDataService.initializePrices();
        });
    }
}
