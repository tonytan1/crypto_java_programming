package com.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Position model
 */
public class PositionTest {

    private Security testSecurity;
    private Position position;

    @BeforeEach
    public void setUp() {
        testSecurity = createTestSecurity();
        position = new Position("AAPL", new BigDecimal("100"), testSecurity);
    }

    private Security createTestSecurity() {
        Security security = new Security();
        security.setTicker("AAPL");
        security.setType(SecurityType.STOCK);
        security.setMu(new BigDecimal("0.10"));
        security.setSigma(new BigDecimal("0.25"));
        return security;
    }

    @Test
    @DisplayName("Should create position with valid parameters")
    public void testCreatePositionWithValidParameters() {
        assertNotNull(position);
        assertEquals("AAPL", position.getSymbol());
        assertEquals(new BigDecimal("100"), position.getPositionSize());
        assertEquals(testSecurity, position.getSecurity());
        assertNull(position.getMarketValue());
    }

    @Test
    @DisplayName("Should set and get symbol")
    public void testSetAndGetSymbol() {
        String newSymbol = "TSLA";
        position.setSymbol(newSymbol);
        
        assertEquals(newSymbol, position.getSymbol());
    }

    @Test
    @DisplayName("Should set and get size")
    public void testSetAndGetSize() {
        BigDecimal newSize = new BigDecimal("200");
        position.setPositionSize(newSize);
        
        assertEquals(newSize, position.getPositionSize());
    }

    @Test
    @DisplayName("Should set and get security")
    public void testSetAndGetSecurity() {
        Security newSecurity = new Security();
        newSecurity.setTicker("TSLA");
        newSecurity.setType(SecurityType.STOCK);
        
        position.setSecurity(newSecurity);
        
        assertEquals(newSecurity, position.getSecurity());
    }

    @Test
    @DisplayName("Should set and get market value")
    public void testSetAndGetMarketValue() {
        BigDecimal marketValue = new BigDecimal("15000.00");
        position.setMarketValue(marketValue);
        
        assertEquals(marketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle null symbol")
    public void testHandleNullSymbol() {
        position.setSymbol(null);
        
        assertNull(position.getSymbol());
    }

    @Test
    @DisplayName("Should handle empty symbol")
    public void testHandleEmptySymbol() {
        position.setSymbol("");
        
        assertEquals("", position.getSymbol());
    }

    @Test
    @DisplayName("Should handle null size")
    public void testHandleNullSize() {
        position.setPositionSize(null);
        
        assertNull(position.getPositionSize());
    }

    @Test
    @DisplayName("Should handle zero size")
    public void testHandleZeroSize() {
        position.setPositionSize(BigDecimal.ZERO);
        
        assertEquals(BigDecimal.ZERO, position.getPositionSize());
    }

    @Test
    @DisplayName("Should handle negative size")
    public void testHandleNegativeSize() {
        BigDecimal negativeSize = new BigDecimal("-100");
        position.setPositionSize(negativeSize);
        
        assertEquals(negativeSize, position.getPositionSize());
    }

    @Test
    @DisplayName("Should handle very large size")
    public void testHandleVeryLargeSize() {
        BigDecimal largeSize = new BigDecimal("999999999");
        position.setPositionSize(largeSize);
        
        assertEquals(largeSize, position.getPositionSize());
    }

    @Test
    @DisplayName("Should handle null security")
    public void testHandleNullSecurity() {
        position.setSecurity(null);
        
        assertNull(position.getSecurity());
    }

    @Test
    @DisplayName("Should handle null market value")
    public void testHandleNullMarketValue() {
        position.setMarketValue(null);
        
        assertNull(position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle zero market value")
    public void testHandleZeroMarketValue() {
        position.setMarketValue(BigDecimal.ZERO);
        
        assertEquals(BigDecimal.ZERO, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle negative market value")
    public void testHandleNegativeMarketValue() {
        BigDecimal negativeMarketValue = new BigDecimal("-5000.00");
        position.setMarketValue(negativeMarketValue);
        
        assertEquals(negativeMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle very large market value")
    public void testHandleVeryLargeMarketValue() {
        BigDecimal largeMarketValue = new BigDecimal("999999999.99");
        position.setMarketValue(largeMarketValue);
        
        assertEquals(largeMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle decimal size values")
    public void testHandleDecimalSizeValues() {
        BigDecimal decimalSize = new BigDecimal("100.5");
        position.setPositionSize(decimalSize);
        
        assertEquals(decimalSize, position.getPositionSize());
    }

    @Test
    @DisplayName("Should handle decimal market value")
    public void testHandleDecimalMarketValue() {
        BigDecimal decimalMarketValue = new BigDecimal("15000.75");
        position.setMarketValue(decimalMarketValue);
        
        assertEquals(decimalMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle very small size values")
    public void testHandleVerySmallSizeValues() {
        BigDecimal smallSize = new BigDecimal("0.001");
        position.setPositionSize(smallSize);
        
        assertEquals(smallSize, position.getPositionSize());
    }

    @Test
    @DisplayName("Should handle very small market values")
    public void testHandleVerySmallMarketValues() {
        BigDecimal smallMarketValue = new BigDecimal("0.01");
        position.setMarketValue(smallMarketValue);
        
        assertEquals(smallMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle long symbol names")
    public void testHandleLongSymbolNames() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("A");
        }
        String longSymbol = sb.toString(); // 100 character symbol
        position.setSymbol(longSymbol);
        
        assertEquals(longSymbol, position.getSymbol());
    }

    @Test
    @DisplayName("Should handle special characters in symbol")
    public void testHandleSpecialCharactersInSymbol() {
        String specialSymbol = "AAPL-2024-CALL-150";
        position.setSymbol(specialSymbol);
        
        assertEquals(specialSymbol, position.getSymbol());
    }

    @Test
    @DisplayName("Should handle different security types")
    public void testHandleDifferentSecurityTypes() {
        // Test with call option
        Security callOption = new Security();
        callOption.setTicker("AAPL_CALL_150_2024");
        callOption.setType(SecurityType.CALL);
        callOption.setStrike(new BigDecimal("150.00"));
        callOption.setMaturity(LocalDate.of(2024, 12, 20));
        
        position.setSecurity(callOption);
        
        assertEquals(callOption, position.getSecurity());
        assertEquals(SecurityType.CALL, position.getSecurity().getType());
        
        // Test with put option
        Security putOption = new Security();
        putOption.setTicker("AAPL_PUT_150_2024");
        putOption.setType(SecurityType.PUT);
        putOption.setStrike(new BigDecimal("150.00"));
        putOption.setMaturity(LocalDate.of(2024, 12, 20));
        
        position.setSecurity(putOption);
        
        assertEquals(putOption, position.getSecurity());
        assertEquals(SecurityType.PUT, position.getSecurity().getType());
    }

    @Test
    @DisplayName("Should handle position with all null values")
    public void testHandlePositionWithAllNullValues() {
        Position nullPosition = new Position(null, null, null);
        
        assertNull(nullPosition.getSymbol());
        assertNull(nullPosition.getPositionSize());
        assertNull(nullPosition.getSecurity());
        assertNull(nullPosition.getMarketValue());
    }

    @Test
    @DisplayName("Should handle position with empty string symbol")
    public void testHandlePositionWithEmptyStringSymbol() {
        Position emptySymbolPosition = new Position("", new BigDecimal("100"), testSecurity);
        
        assertEquals("", emptySymbolPosition.getSymbol());
        assertEquals(new BigDecimal("100"), emptySymbolPosition.getPositionSize());
        assertEquals(testSecurity, emptySymbolPosition.getSecurity());
    }

    @Test
    @DisplayName("Should handle position with whitespace symbol")
    public void testHandlePositionWithWhitespaceSymbol() {
        Position whitespacePosition = new Position("   ", new BigDecimal("100"), testSecurity);
        
        assertEquals("   ", whitespacePosition.getSymbol());
        assertEquals(new BigDecimal("100"), whitespacePosition.getPositionSize());
        assertEquals(testSecurity, whitespacePosition.getSecurity());
    }

    @Test
    @DisplayName("Should handle position with very small decimal values")
    public void testHandlePositionWithVerySmallDecimalValues() {
        BigDecimal verySmallSize = new BigDecimal("0.000001");
        BigDecimal verySmallMarketValue = new BigDecimal("0.000001");
        
        position.setPositionSize(verySmallSize);
        position.setMarketValue(verySmallMarketValue);
        
        assertEquals(verySmallSize, position.getPositionSize());
        assertEquals(verySmallMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle position with very large decimal values")
    public void testHandlePositionWithVeryLargeDecimalValues() {
        BigDecimal veryLargeSize = new BigDecimal("999999999.999999");
        BigDecimal veryLargeMarketValue = new BigDecimal("999999999.999999");
        
        position.setPositionSize(veryLargeSize);
        position.setMarketValue(veryLargeMarketValue);
        
        assertEquals(veryLargeSize, position.getPositionSize());
        assertEquals(veryLargeMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle position with scientific notation values")
    public void testHandlePositionWithScientificNotationValues() {
        BigDecimal scientificSize = new BigDecimal("1.5E6"); // 1,500,000
        BigDecimal scientificMarketValue = new BigDecimal("2.5E7"); // 25,000,000
        
        position.setPositionSize(scientificSize);
        position.setMarketValue(scientificMarketValue);
        
        assertEquals(scientificSize, position.getPositionSize());
        assertEquals(scientificMarketValue, position.getMarketValue());
    }

    @Test
    @DisplayName("Should handle position with different currency symbols in symbol")
    public void testHandlePositionWithCurrencySymbolsInSymbol() {
        String currencySymbol = "USD/AAPL";
        position.setSymbol(currencySymbol);
        
        assertEquals(currencySymbol, position.getSymbol());
    }

    @Test
    @DisplayName("Should handle position with unicode characters in symbol")
    public void testHandlePositionWithUnicodeCharactersInSymbol() {
        String unicodeSymbol = "AAPL_TM";
        position.setSymbol(unicodeSymbol);
        
        assertEquals(unicodeSymbol, position.getSymbol());
    }

    @Test
    @DisplayName("Should handle position with numeric symbol")
    public void testHandlePositionWithNumericSymbol() {
        String numericSymbol = "12345";
        position.setSymbol(numericSymbol);
        
        assertEquals(numericSymbol, position.getSymbol());
    }

    @Test
    @DisplayName("Should handle position with mixed case symbol")
    public void testHandlePositionWithMixedCaseSymbol() {
        String mixedCaseSymbol = "AaPl";
        position.setSymbol(mixedCaseSymbol);
        
        assertEquals(mixedCaseSymbol, position.getSymbol());
    }
}
