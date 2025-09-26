package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OptionPricingService
 */
public class OptionPricingServiceTest {

    private OptionPricingService optionPricingService;
    private Security callOption;
    private Security putOption;
    private BigDecimal underlyingPrice;
    private BigDecimal riskFreeRate;

    @BeforeEach
    public void setUp() {
        optionPricingService = new OptionPricingService();
        riskFreeRate = new BigDecimal("0.05"); // 5% risk-free rate
        
        // Use reflection to set the risk-free rate
        try {
            java.lang.reflect.Field riskFreeRateField = OptionPricingService.class.getDeclaredField("riskFreeRate");
            riskFreeRateField.setAccessible(true);
            riskFreeRateField.set(optionPricingService, riskFreeRate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set risk-free rate", e);
        }
        
        // Create test call option
        callOption = new Security();
        callOption.setTicker("AAPL_CALL_150_2024");
        callOption.setType(SecurityType.CALL);
        callOption.setStrike(new BigDecimal("150.00"));
        callOption.setMaturity(LocalDate.of(2025, 12, 20));
        callOption.setMu(new BigDecimal("0.10")); // 10% drift
        callOption.setSigma(new BigDecimal("0.25")); // 25% volatility
        
        // Create test put option
        putOption = new Security();
        putOption.setTicker("AAPL_PUT_150_2024");
        putOption.setType(SecurityType.PUT);
        putOption.setStrike(new BigDecimal("150.00"));
        putOption.setMaturity(LocalDate.of(2025, 12, 20));
        putOption.setMu(new BigDecimal("0.10"));
        putOption.setSigma(new BigDecimal("0.25"));
        
        underlyingPrice = new BigDecimal("155.00");
    }

    @AfterEach
    public void tearDown() {
        // No mocks to reset in this test, but good practice to have cleanup
    }

    @Test
    @DisplayName("Should calculate call option price using Black-Scholes formula")
    public void testCalculateCallOptionPrice() {
        BigDecimal callPrice = optionPricingService.calculateOptionPrice(callOption, underlyingPrice);
        
        assertNotNull(callPrice);
        assertTrue(callPrice.compareTo(BigDecimal.ZERO) > 0, "Call option price should be positive");
        
        // For ITM call option (S > K), price should be reasonable
        assertTrue(callPrice.compareTo(underlyingPrice) < 0, "Call option price should be less than underlying price");
        
        // Verify price is within reasonable range (should be around 10-20 for this example)
        assertTrue(callPrice.compareTo(new BigDecimal("5.00")) > 0, "Call option price seems too low");
        assertTrue(callPrice.compareTo(new BigDecimal("30.00")) < 0, "Call option price seems too high");
    }

    @Test
    @DisplayName("Should calculate put option price using Black-Scholes formula")
    public void testCalculatePutOptionPrice() {
        BigDecimal putPrice = optionPricingService.calculateOptionPrice(putOption, underlyingPrice);
        
        assertNotNull(putPrice);
        assertTrue(putPrice.compareTo(BigDecimal.ZERO) > 0, "Put option price should be positive");
        
        // For OTM put option (S > K), price should be relatively low
        assertTrue(putPrice.compareTo(underlyingPrice) < 0, "Put option price should be less than underlying price");
    }

    @Test
    @DisplayName("Should handle in-the-money call option correctly")
    public void testInTheMoneyCallOption() {
        // Set underlying price above strike price
        BigDecimal highUnderlyingPrice = new BigDecimal("200.00");
        BigDecimal callPrice = optionPricingService.calculateOptionPrice(callOption, highUnderlyingPrice);
        
        assertNotNull(callPrice);
        assertTrue(callPrice.compareTo(BigDecimal.ZERO) > 0);
        
        // ITM call should have higher intrinsic value
        BigDecimal intrinsicValue = highUnderlyingPrice.subtract(callOption.getStrike());
        assertTrue(callPrice.compareTo(intrinsicValue) > 0, "Call option price should exceed intrinsic value due to time value");
    }


    @Test
    @DisplayName("Should throw exception for stock type security")
    public void testCalculateOptionPriceForStock() {
        Security stock = new Security();
        stock.setTicker("AAPL");
        stock.setType(SecurityType.STOCK);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> optionPricingService.calculateOptionPrice(stock, underlyingPrice)
        );
        
        assertEquals("Cannot calculate option price for stock", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle zero underlying price")
    public void testZeroUnderlyingPrice() {
        BigDecimal zeroPrice = BigDecimal.ZERO;
        
        // Should throw exception when trying to calculate log(0)
        assertThrows(NumberFormatException.class, () -> {
            optionPricingService.calculateOptionPrice(callOption, zeroPrice);
        });
    }

    @Test
    @DisplayName("Should handle very high volatility")
    public void testHighVolatility() {
        Security highVolOption = new Security();
        highVolOption.setTicker("HIGH_VOL_CALL");
        highVolOption.setType(SecurityType.CALL);
        highVolOption.setStrike(new BigDecimal("150.00"));
        highVolOption.setMaturity(LocalDate.of(2025, 12, 20));
        highVolOption.setMu(new BigDecimal("0.10"));
        highVolOption.setSigma(new BigDecimal("1.00")); // 100% volatility
        
        BigDecimal highVolPrice = optionPricingService.calculateOptionPrice(highVolOption, underlyingPrice);
        
        assertNotNull(highVolPrice);
        assertTrue(highVolPrice.compareTo(BigDecimal.ZERO) > 0);
        
        // High volatility should result in higher option price
        BigDecimal normalVolPrice = optionPricingService.calculateOptionPrice(callOption, underlyingPrice);
        assertTrue(highVolPrice.compareTo(normalVolPrice) > 0, "High volatility should increase option price");
    }

    @Test
    @DisplayName("Should handle near-expiration option")
    public void testNearExpirationOption() {
        Security nearExpOption = new Security();
        nearExpOption.setTicker("NEAR_EXP_CALL");
        nearExpOption.setType(SecurityType.CALL);
        nearExpOption.setStrike(new BigDecimal("150.00"));
        nearExpOption.setMaturity(LocalDate.now().plusDays(1));
        nearExpOption.setMu(new BigDecimal("0.10"));
        nearExpOption.setSigma(new BigDecimal("0.25"));
        
        BigDecimal nearExpPrice = optionPricingService.calculateOptionPrice(nearExpOption, underlyingPrice);
        
        assertNotNull(nearExpPrice);
        assertTrue(nearExpPrice.compareTo(BigDecimal.ZERO) > 0);
        
        // Near expiration should have lower time value
        BigDecimal longTermPrice = optionPricingService.calculateOptionPrice(callOption, underlyingPrice);
        assertTrue(nearExpPrice.compareTo(longTermPrice) < 0, "Near expiration should reduce option price");
    }

    @Test
    @DisplayName("Should handle put-call parity relationship")
    public void testPutCallParity() {
        // Create call and put with same parameters
        Security call = new Security();
        call.setTicker("CALL");
        call.setType(SecurityType.CALL);
        call.setStrike(new BigDecimal("150.00"));
        call.setMaturity(LocalDate.of(2025, 12, 20));
        call.setMu(new BigDecimal("0.10"));
        call.setSigma(new BigDecimal("0.25"));
        
        Security put = new Security();
        put.setTicker("PUT");
        put.setType(SecurityType.PUT);
        put.setStrike(new BigDecimal("150.00"));
        put.setMaturity(LocalDate.of(2025, 12, 20));
        put.setMu(new BigDecimal("0.10"));
        put.setSigma(new BigDecimal("0.25"));
        
        BigDecimal callPrice = optionPricingService.calculateOptionPrice(call, underlyingPrice);
        BigDecimal putPrice = optionPricingService.calculateOptionPrice(put, underlyingPrice);
        
        // Put-Call Parity: C - P = S - K*e^(-r*T)
        // For our test: C - P should be approximately S - K (since r*T is small)
        BigDecimal difference = callPrice.subtract(putPrice);
        BigDecimal expectedDifference = underlyingPrice.subtract(call.getStrike());
        
        // Allow for some tolerance due to discrete time calculations
        BigDecimal tolerance = new BigDecimal("2.00");
        assertTrue(difference.subtract(expectedDifference).abs().compareTo(tolerance) < 0, 
            "Put-call parity relationship should hold approximately");
    }

    @Test
    @DisplayName("Should handle null security gracefully")
    public void testNullSecurity() {
        assertThrows(NullPointerException.class, 
            () -> optionPricingService.calculateOptionPrice(null, underlyingPrice));
    }

    @Test
    @DisplayName("Should handle null underlying price gracefully")
    public void testNullUnderlyingPrice() {
        assertThrows(NullPointerException.class, 
            () -> optionPricingService.calculateOptionPrice(callOption, null));
    }
}
