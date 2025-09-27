package com.portfolio.service;

import com.portfolio.model.Position;
import com.portfolio.model.Security;
import com.portfolio.repository.SecurityRepository;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for loading portfolio positions from CSV files.
 */
@Service
public class PositionLoaderService {
    
    private static final Logger logger = Logger.getLogger(PositionLoaderService.class.getName());
    
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
            logger.severe("Invalid CSV file path: " + filePath);
            throw new IOException("Invalid CSV file path");
        }
        
        // Basic path traversal protection - only check for directory traversal attempts
        if (filePath.contains("..")) {
            logger.severe("Potentially unsafe file path detected: " + filePath);
            throw new IOException("Unsafe file path detected");
        }
        
        logger.info("Loading positions from CSV file: " + filePath);
        List<Position> positions = new ArrayList<>();
        int validLines = 0;
        int invalidLines = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Skip empty lines
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                
                // Parse CSV line (simple comma splitting)
                String[] fields = line.split(",", -1);
                
                // Basic validation
                if (fields.length < 2 || !StringUtils.hasText(fields[0]) || !StringUtils.hasText(fields[1])) {
                    logger.warning("Skipping invalid line " + lineNumber + ": missing required data");
                    invalidLines++;
                    continue;
                }
                
                try {
                    String symbol = fields[0].trim().toUpperCase();
                    
                    // Simple ticker validation
                    if (!TICKER_PATTERN.matcher(symbol).matches()) {
                        logger.warning("Invalid ticker format at line " + lineNumber + ": " + symbol);
                        invalidLines++;
                        continue;
                    }
                    
                    // Parse and validate position size
                    BigDecimal positionSize;
                    try {
                        positionSize = new BigDecimal(fields[1].trim());
                        if (positionSize.compareTo(MIN_POSITION_SIZE) < 0 || positionSize.compareTo(MAX_POSITION_SIZE) > 0) {
                            logger.warning("Position size out of range at line " + lineNumber + ": " + positionSize);
                            invalidLines++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid numeric format at line " + lineNumber + ": " + fields[1]);
                        invalidLines++;
                        continue;
                    }
                    
                    // Look up security (JdbcTemplate automatically prevents SQL injection with parameterized queries)
                    Optional<Security> securityOpt = securityRepository.findByTicker(symbol);
                    Security security = securityOpt.orElse(null);
                    
                    if (security == null) {
                        logger.warning("No security definition found for symbol: " + symbol + " at line " + lineNumber);
                    }
                    
                    Position position = new Position(symbol, positionSize, security);
                    positions.add(position);
                    validLines++;
                    logger.fine("Loaded position: " + symbol + " with size " + positionSize);
                    
                } catch (Exception e) {
                    logger.severe("Unexpected error processing line " + lineNumber + ": " + e.getMessage());
                    invalidLines++;
                }
            }
        }
        
        logger.info("CSV loading completed: " + validLines + " valid positions, " + invalidLines + " invalid lines skipped");
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
