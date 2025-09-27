package com.portfolio.service;

import com.portfolio.model.Position;
import com.portfolio.model.Security;
import com.portfolio.repository.SecurityRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for loading portfolio positions from CSV files.
 */
@Service
public class PositionLoaderService {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionLoaderService.class);
    
    // Simple validation patterns
    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Z0-9\\-_]{1,50}$");
    private static final BigDecimal MAX_POSITION_SIZE = new BigDecimal("999999999");
    private static final BigDecimal MIN_POSITION_SIZE = new BigDecimal("-999999999");
    
    @Autowired
    private SecurityRepository securityRepository;
    
    @Value("${portfolio.csv.file:src/main/resources/sample-positions.csv}")
    private String csvFilePath;
    
    /**
     * Loads positions from the configured CSV file
     */
    public List<Position> loadPositions() throws IOException {
        return loadPositions(csvFilePath);
    }
    
    /**
     * Loads positions from a specific CSV file
     */
    public List<Position> loadPositions(String filePath) throws IOException {
        // Simple file path validation using Spring's StringUtils
        if (!StringUtils.hasText(filePath) || !filePath.toLowerCase().endsWith(".csv")) {
            logger.error("Invalid CSV file path: {}", filePath);
            throw new IOException("Invalid CSV file path");
        }
        
        // Basic path traversal protection - only check for directory traversal attempts
        if (filePath.contains("..")) {
            logger.error("Potentially unsafe file path detected: {}", filePath);
            throw new IOException("Unsafe file path detected");
        }
        
        logger.info("Loading positions from CSV file: {}", filePath);
        List<Position> positions = new ArrayList<>();
        int validLines = 0;
        int invalidLines = 0;
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            boolean isFirstLine = true;
            int lineNumber = 0;
            
            while ((nextLine = reader.readNext()) != null) {
                lineNumber++;
                
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Basic validation
                if (nextLine.length < 2 || !StringUtils.hasText(nextLine[0]) || !StringUtils.hasText(nextLine[1])) {
                    logger.warn("Skipping invalid line {}: missing required data", lineNumber);
                    invalidLines++;
                    continue;
                }
                
                try {
                    String symbol = nextLine[0].trim().toUpperCase();
                    
                    // Simple ticker validation
                    if (!TICKER_PATTERN.matcher(symbol).matches()) {
                        logger.warn("Invalid ticker format at line {}: {}", lineNumber, symbol);
                        invalidLines++;
                        continue;
                    }
                    
                    // Parse and validate position size
                    BigDecimal positionSize;
                    try {
                        positionSize = new BigDecimal(nextLine[1].trim());
                        if (positionSize.compareTo(MIN_POSITION_SIZE) < 0 || positionSize.compareTo(MAX_POSITION_SIZE) > 0) {
                            logger.warn("Position size out of range at line {}: {}", lineNumber, positionSize);
                            invalidLines++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid numeric format at line {}: {}", lineNumber, nextLine[1]);
                        invalidLines++;
                        continue;
                    }
                    
                    // Look up security (JdbcTemplate automatically prevents SQL injection with parameterized queries)
                    Optional<Security> securityOpt = securityRepository.findByTicker(symbol);
                    Security security = securityOpt.orElse(null);
                    
                    if (security == null) {
                        logger.warn("No security definition found for symbol: {} at line {}", symbol, lineNumber);
                    }
                    
                    Position position = new Position(symbol, positionSize, security);
                    positions.add(position);
                    validLines++;
                    logger.debug("Loaded position: {} with size {}", symbol, positionSize);
                    
                } catch (Exception e) {
                    logger.error("Unexpected error processing line {}: {}", lineNumber, e.getMessage(), e);
                    invalidLines++;
                }
            }
        } catch (CsvValidationException e) {
            logger.error("CSV validation error: {}", e.getMessage(), e);
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        }
        
        logger.info("CSV loading completed: {} valid positions, {} invalid lines skipped", validLines, invalidLines);
        return positions;
    }
    
    /**
     * Validates that all positions have corresponding security definitions
     */
    public List<String> validatePositions(List<Position> positions) {
        List<String> missingSecurities = new ArrayList<>();
        
        for (Position position : positions) {
            if (position.getSecurity() == null) {
                missingSecurities.add(position.getSymbol());
            }
        }
        
        return missingSecurities;
    }
}
