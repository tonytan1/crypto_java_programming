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

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for loading portfolio positions from CSV files.
 */
@Service
public class PositionLoaderService {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionLoaderService.class);
    
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
        logger.info("Loading positions from CSV file: {}", filePath);
        List<Position> positions = new ArrayList<>();
        
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
                
                if (nextLine.length >= 2) {
                    String symbol = nextLine[0].trim();
                    BigDecimal positionSize = new BigDecimal(nextLine[1].trim());
                    
                    // Look up security definition
                    Optional<Security> securityOpt = securityRepository.findByTicker(symbol);
                    Security security = securityOpt.orElse(null);
                    
                    if (security == null) {
                        logger.warn("No security definition found for symbol: {} at line {}", symbol, lineNumber);
                    }
                    
                    Position position = new Position(symbol, positionSize, security);
                    positions.add(position);
                    logger.debug("Loaded position: {} with size {}", symbol, positionSize);
                } else {
                    logger.warn("Skipping invalid line {}: insufficient columns", lineNumber);
                }
            }
        } catch (CsvValidationException e) {
            logger.error("CSV validation error: {}", e.getMessage(), e);
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        }
        
        logger.info("Successfully loaded {} positions from CSV file", positions.size());
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
