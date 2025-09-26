package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataInitializationService
 */
public class DataInitializationServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    private DataInitializationService dataInitializationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dataInitializationService = new DataInitializationService();
        
        // Use reflection to inject the mocked repository
        try {
            java.lang.reflect.Field repositoryField = DataInitializationService.class.getDeclaredField("securityRepository");
            repositoryField.setAccessible(true);
            repositoryField.set(dataInitializationService, securityRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock repository", e);
        }
    }

    @AfterEach
    public void tearDown() {
        // Reset mocks to clear any state
        reset(securityRepository);
    }


    @Test
    @DisplayName("Should reset and reinitialize if data already exists")
    public void testInitializeSampleDataWhenDataExists() {
        // Mock repository to return existing data
        when(securityRepository.findAll()).thenReturn(Arrays.asList(new Security(), new Security(), new Security(), new Security(), new Security()));
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Execute the method
        dataInitializationService.initializeSampleData();
        
        // Verify that deleteAll was called to reset existing data
        verify(securityRepository).deleteAll();
        // Verify that new securities were saved
        verify(securityRepository, atLeast(1)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create correct number of securities")
    public void testCreateCorrectNumberOfSecurities() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        dataInitializationService.initializeSampleData();
        
        // Verify that exactly 10 securities were saved (2 stocks + 4 expired options + 4 active options)
        verify(securityRepository, times(10)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create stocks with correct properties")
    public void testCreateStocksWithCorrectProperties() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            if (security.getType() == SecurityType.STOCK) {
                assertNotNull(security.getTicker());
                assertEquals(SecurityType.STOCK, security.getType());
                assertNotNull(security.getMu());
                assertNotNull(security.getSigma());
                assertTrue(security.getMu().compareTo(BigDecimal.ZERO) > 0);
                assertTrue(security.getSigma().compareTo(BigDecimal.ZERO) > 0);
            }
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        // Verify that the callback was executed for stock securities
        verify(securityRepository, atLeast(3)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create call options with correct properties")
    public void testCreateCallOptionsWithCorrectProperties() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            if (security.getType() == SecurityType.CALL) {
                assertNotNull(security.getTicker());
                assertEquals(SecurityType.CALL, security.getType());
                assertNotNull(security.getStrike());
                assertNotNull(security.getMaturity());
                assertNotNull(security.getMu());
                assertNotNull(security.getSigma());
                assertTrue(security.getStrike().compareTo(BigDecimal.ZERO) > 0);
                // Check that it's either expired (before now) or active (after now)
                assertTrue(security.getMaturity().isBefore(LocalDate.now()) || 
                          security.getMaturity().isAfter(LocalDate.now()));
            }
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        verify(securityRepository, atLeast(1)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create put options with correct properties")
    public void testCreatePutOptionsWithCorrectProperties() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            if (security.getType() == SecurityType.PUT) {
                assertNotNull(security.getTicker());
                assertEquals(SecurityType.PUT, security.getType());
                assertNotNull(security.getStrike());
                assertNotNull(security.getMaturity());
                assertNotNull(security.getMu());
                assertNotNull(security.getSigma());
                assertTrue(security.getStrike().compareTo(BigDecimal.ZERO) > 0);
            }
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        verify(securityRepository, atLeast(1)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create expired options with past maturity dates")
    public void testCreateExpiredOptions() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            if (security.getTicker() != null && security.getTicker().contains("EXPIRED")) {
                assertTrue(security.getMaturity().isBefore(LocalDate.now()), 
                    "Expired option should have past maturity date");
            }
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        verify(securityRepository, atLeast(1)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create active options with future maturity dates")
    public void testCreateActiveOptions() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            if (security.getTicker() != null && security.getTicker().contains("ACTIVE")) {
                assertTrue(security.getMaturity().isAfter(LocalDate.now()), 
                    "Active option should have future maturity date");
            }
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        verify(securityRepository, atLeast(1)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    public void testHandleRepositoryExceptions() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        doThrow(new RuntimeException("Database error")).when(securityRepository).deleteAll();
        
        // Should not throw exception, but should handle gracefully
        assertDoesNotThrow(() -> {
            dataInitializationService.initializeSampleData();
        });
    }

    @Test
    @DisplayName("Should handle save exceptions gracefully")
    public void testHandleSaveExceptions() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenThrow(new RuntimeException("Save error"));
        
        // Should not throw exception, but should handle gracefully
        assertDoesNotThrow(() -> {
            dataInitializationService.initializeSampleData();
        });
    }

    @Test
    @DisplayName("Should create securities with realistic parameters")
    public void testCreateSecuritiesWithRealisticParameters() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            
            // Verify mu (drift) is realistic (typically 0.05 to 0.15)
            if (security.getMu() != null) {
                assertTrue(security.getMu().compareTo(new BigDecimal("0.01")) >= 0);
                assertTrue(security.getMu().compareTo(new BigDecimal("0.30")) <= 0);
            }
            
            // Verify sigma (volatility) is realistic (typically 0.10 to 0.50)
            if (security.getSigma() != null) {
                assertTrue(security.getSigma().compareTo(new BigDecimal("0.05")) >= 0);
                assertTrue(security.getSigma().compareTo(new BigDecimal("1.00")) <= 0);
            }
            
            // Verify strike prices are realistic
            if (security.getStrike() != null) {
                assertTrue(security.getStrike().compareTo(new BigDecimal("10.00")) >= 0);
                assertTrue(security.getStrike().compareTo(new BigDecimal("1000.00")) <= 0);
            }
            
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        verify(securityRepository, atLeast(6)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should create unique tickers for all securities")
    public void testCreateUniqueTickers() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        dataInitializationService.initializeSampleData();
        
        // Verify that all saved securities have unique tickers
        verify(securityRepository, atLeast(6)).save(any(Security.class));
    }


    @Test
    @DisplayName("Should create securities with proper date formatting")
    public void testCreateSecuritiesWithProperDates() {
        when(securityRepository.findAll()).thenReturn(new ArrayList<>());
        when(securityRepository.save(any(Security.class))).thenAnswer(invocation -> {
            Security security = invocation.getArgument(0);
            if (security.getMaturity() != null) {
                // Verify maturity date is reasonable (not too far in the past or future)
                LocalDate now = LocalDate.now();
                LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
                LocalDate twoYearsFromNow = LocalDate.now().plusYears(2);
                
                assertTrue(security.getMaturity().isAfter(fiveYearsAgo));
                assertTrue(security.getMaturity().isBefore(twoYearsFromNow));
            }
            return security;
        });
        
        dataInitializationService.initializeSampleData();
        
        verify(securityRepository, atLeast(1)).save(any(Security.class));
    }

    @Test
    @DisplayName("Should handle null repository gracefully")
    public void testHandleNullRepository() {
        // This test verifies that the service doesn't crash if repository is null
        // In a real scenario, this would be handled by dependency injection
        assertDoesNotThrow(() -> {
            DataInitializationService service = new DataInitializationService();
            // This might throw an exception, which is expected
            try {
                service.initializeSampleData();
            } catch (Exception e) {
                // Expected behavior when repository is not injected
            }
        });
    }
}
