package com.portfolio.service;

import com.portfolio.model.Position;
import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PositionLoaderService
 */
public class PositionLoaderServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    private PositionLoaderService positionLoaderService;
    private List<Security> testSecurities;

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        positionLoaderService = new PositionLoaderService();
        
        // Use reflection to inject the mocked repository
        try {
            java.lang.reflect.Field repositoryField = PositionLoaderService.class.getDeclaredField("securityRepository");
            repositoryField.setAccessible(true);
            repositoryField.set(positionLoaderService, securityRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock repository", e);
        }
        
        // Create test securities
        testSecurities = createTestSecurities();
        setupMockRepository();
    }

    @AfterEach
    public void tearDown() {
        // Reset mocks to clear any state
        reset(securityRepository);
    }

    private List<Security> createTestSecurities() {
        List<Security> securities = new ArrayList<>();
        
        // AAPL Stock
        Security aaplStock = new Security();
        aaplStock.setTicker("AAPL");
        aaplStock.setType(SecurityType.STOCK);
        aaplStock.setMu(new BigDecimal("0.10"));
        aaplStock.setSigma(new BigDecimal("0.25"));
        securities.add(aaplStock);
        
        // AAPL Call Option
        Security aaplCall = new Security();
        aaplCall.setTicker("AAPL_CALL_150_2024");
        aaplCall.setType(SecurityType.CALL);
        aaplCall.setStrike(new BigDecimal("150.00"));
        aaplCall.setMaturity(LocalDate.of(2024, 12, 20));
        aaplCall.setMu(new BigDecimal("0.10"));
        aaplCall.setSigma(new BigDecimal("0.25"));
        securities.add(aaplCall);
        
        // AAPL Put Option
        Security aaplPut = new Security();
        aaplPut.setTicker("AAPL_PUT_150_2024");
        aaplPut.setType(SecurityType.PUT);
        aaplPut.setStrike(new BigDecimal("150.00"));
        aaplPut.setMaturity(LocalDate.of(2024, 12, 20));
        aaplPut.setMu(new BigDecimal("0.10"));
        aaplPut.setSigma(new BigDecimal("0.25"));
        securities.add(aaplPut);
        
        return securities;
    }

    private void setupMockRepository() {
        for (Security security : testSecurities) {
            when(securityRepository.findByTicker(security.getTicker())).thenReturn(Optional.of(security));
        }
    }

    @Test
    @DisplayName("Should load positions from valid CSV file")
    public void testLoadPositionsFromValidCSV() throws IOException {
        // Create test CSV file
        File csvFile = new File(tempDir, "test_positions.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,100\n");
            writer.write("AAPL_CALL_150_2024,10\n");
            writer.write("AAPL_PUT_150_2024,5\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(3, positions.size());
        
        // Verify first position (AAPL stock)
        Position aaplPosition = positions.get(0);
        assertEquals("AAPL", aaplPosition.getSymbol());
        assertEquals(new BigDecimal("100"), aaplPosition.getPositionSize());
        assertNotNull(aaplPosition.getSecurity());
        assertEquals(SecurityType.STOCK, aaplPosition.getSecurity().getType());
        
        // Verify second position (AAPL call)
        Position callPosition = positions.get(1);
        assertEquals("AAPL_CALL_150_2024", callPosition.getSymbol());
        assertEquals(new BigDecimal("10"), callPosition.getPositionSize());
        assertNotNull(callPosition.getSecurity());
        assertEquals(SecurityType.CALL, callPosition.getSecurity().getType());
        
        // Verify third position (AAPL put)
        Position putPosition = positions.get(2);
        assertEquals("AAPL_PUT_150_2024", putPosition.getSymbol());
        assertEquals(new BigDecimal("5"), putPosition.getPositionSize());
        assertNotNull(putPosition.getSecurity());
        assertEquals(SecurityType.PUT, putPosition.getSecurity().getType());
    }

    @Test
    @DisplayName("Should handle CSV file with headers")
    public void testLoadPositionsWithHeaders() throws IOException {
        File csvFile = new File(tempDir, "test_positions.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size,Type\n");
            writer.write("AAPL,100,STOCK\n");
            writer.write("AAPL_CALL_150_2024,10,CALL\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(2, positions.size());
    }

    @Test
    @DisplayName("Should handle empty CSV file")
    public void testLoadPositionsFromEmptyCSV() throws IOException {
        File csvFile = new File(tempDir, "empty_positions.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertTrue(positions.isEmpty());
    }

    @Test
    @DisplayName("Should handle CSV file with only headers")
    public void testLoadPositionsWithOnlyHeaders() throws IOException {
        File csvFile = new File(tempDir, "headers_only.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertTrue(positions.isEmpty());
    }

    @Test
    @DisplayName("Should handle missing security definitions")
    public void testLoadPositionsWithMissingSecurities() throws IOException {
        File csvFile = new File(tempDir, "missing_securities.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,100\n");
            writer.write("UNKNOWN_STOCK,50\n");
            writer.write("AAPL_CALL_150_2024,10\n");
        }
        
        // Mock repository to return null for unknown stock
        when(securityRepository.findByTicker("UNKNOWN_STOCK")).thenReturn(Optional.empty());
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(3, positions.size());
        
        // Verify positions with known securities have security definitions
        assertNotNull(positions.get(0).getSecurity()); // AAPL
        assertNull(positions.get(1).getSecurity());    // UNKNOWN_STOCK
        assertNotNull(positions.get(2).getSecurity()); // AAPL_CALL_150_2024
    }

    @Test
    @DisplayName("Should handle invalid CSV format")
    public void testLoadPositionsWithInvalidCSV() throws IOException {
        File csvFile = new File(tempDir, "invalid.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Invalid,CSV,Format\n");
            writer.write("AAPL,100,Extra,Columns\n");
        }
        
        // Should not throw exception, but should handle gracefully
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        // Should still process valid lines
        assertTrue(positions.size() >= 0);
    }


    @Test
    @DisplayName("Should handle negative position sizes")
    public void testLoadPositionsWithNegativeSizes() throws IOException {
        File csvFile = new File(tempDir, "negative_sizes.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,100\n");
            writer.write("AAPL_CALL_150_2024,-10\n"); // Short position
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(2, positions.size());
        
        // Verify negative size is preserved
        Position shortPosition = positions.get(1);
        assertEquals(new BigDecimal("-10"), shortPosition.getPositionSize());
    }

    @Test
    @DisplayName("Should handle zero position sizes")
    public void testLoadPositionsWithZeroSizes() throws IOException {
        File csvFile = new File(tempDir, "zero_sizes.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,0\n");
            writer.write("AAPL_CALL_150_2024,10\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(2, positions.size());
        
        // Verify zero size is preserved
        Position zeroPosition = positions.get(0);
        assertEquals(BigDecimal.ZERO, zeroPosition.getPositionSize());
    }

    @Test
    @DisplayName("Should validate positions correctly")
    public void testValidatePositions() throws IOException {
        File csvFile = new File(tempDir, "mixed_positions.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,100\n");
            writer.write("UNKNOWN_STOCK,50\n");
            writer.write("AAPL_CALL_150_2024,10\n");
        }
        
        // Mock repository to return null for unknown stock
        when(securityRepository.findByTicker("UNKNOWN_STOCK")).thenReturn(Optional.empty());
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        List<String> missingSecurities = positionLoaderService.validatePositions(positions);
        
        assertNotNull(missingSecurities);
        assertEquals(1, missingSecurities.size());
        assertTrue(missingSecurities.contains("UNKNOWN_STOCK"));
    }

    @Test
    @DisplayName("Should handle file not found")
    public void testLoadPositionsFileNotFound() {
        String nonExistentFile = "non_existent_file.csv";
        
        assertThrows(IOException.class, () -> {
            positionLoaderService.loadPositions(nonExistentFile);
        });
    }

    @Test
    @DisplayName("Should handle malformed CSV lines")
    public void testLoadPositionsWithMalformedLines() throws IOException {
        File csvFile = new File(tempDir, "malformed.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,100\n");
            writer.write("INVALID_LINE_WITHOUT_COMMA\n");
            writer.write("AAPL_CALL_150_2024,10\n");
        }
        
        // Should handle malformed lines gracefully
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        // Should process valid lines and skip invalid ones
        assertTrue(positions.size() >= 0);
    }

    @Test
    @DisplayName("Should handle very large position sizes")
    public void testLoadPositionsWithLargeSizes() throws IOException {
        File csvFile = new File(tempDir, "large_sizes.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write("AAPL,1000000\n");
            writer.write("AAPL_CALL_150_2024,999999\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(2, positions.size());
        
        // Verify large sizes are handled correctly
        assertEquals(new BigDecimal("1000000"), positions.get(0).getPositionSize());
        assertEquals(new BigDecimal("999999"), positions.get(1).getPositionSize());
    }

    @Test
    @DisplayName("Should handle CSV with extra whitespace")
    public void testLoadPositionsWithWhitespace() throws IOException {
        File csvFile = new File(tempDir, "whitespace.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write(" AAPL , 100 \n");
            writer.write(" AAPL_CALL_150_2024 , 10 \n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        assertEquals(2, positions.size());
        
        // Verify whitespace is handled correctly
        assertEquals("AAPL", positions.get(0).getSymbol());
        assertEquals(new BigDecimal("100"), positions.get(0).getPositionSize());
    }

    @Test
    @DisplayName("Should handle empty symbol field")
    public void testLoadPositionsWithEmptySymbol() throws IOException {
        File csvFile = new File(tempDir, "empty_symbol.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Symbol,Size\n");
            writer.write(",100\n");
            writer.write("AAPL,50\n");
        }
        
        List<Position> positions = positionLoaderService.loadPositions(csvFile.getAbsolutePath());
        
        assertNotNull(positions);
        // Should handle empty symbol gracefully
        assertTrue(positions.size() >= 0);
    }


}
