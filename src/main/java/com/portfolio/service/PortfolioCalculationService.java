package com.portfolio.service;

import com.portfolio.model.Position;
import com.portfolio.model.Portfolio;
import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for calculating portfolio values and NAV.
 * Handles both stock and option pricing calculations.
 */
@Service
public class PortfolioCalculationService {
    
    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    private OptionPricingService optionPricingService;
    
    /**
     * Calculates market values for all positions in the portfolio
     */
    public void calculatePortfolioValues(Portfolio portfolio) {
        List<Position> positions = portfolio.getPositions();
        
        for (Position position : positions) {
            calculatePositionValue(position);
        }
        
        // Recalculate total NAV
        portfolio.calculateNAV();
    }
    
    /**
     * Calculates the market value for a single position
     */
    public void calculatePositionValue(Position position) {
        Security security = position.getSecurity();
        if (security == null) {
            position.setCurrentPrice(BigDecimal.ZERO);
            position.setMarketValue(BigDecimal.ZERO);
            return;
        }
        
        BigDecimal currentPrice;
        
        if (security.getType() == SecurityType.STOCK) {
            // For stocks, get current market price
            currentPrice = marketDataService.getCurrentPrice(security.getTicker());
        } else {
            // For options, calculate theoretical price
            String underlyingTicker = extractUnderlyingTicker(security.getTicker());
            BigDecimal underlyingPrice = marketDataService.getCurrentPrice(underlyingTicker);
            
            if (underlyingPrice.compareTo(BigDecimal.ZERO) > 0) {
                currentPrice = optionPricingService.calculateOptionPrice(security, underlyingPrice);
            } else {
                currentPrice = BigDecimal.ZERO;
            }
        }
        
        position.setCurrentPrice(currentPrice);
        position.setMarketValue(position.calculateMarketValue());
    }
    
    /**
     * Extracts the underlying stock ticker from an option ticker
     * Example: "AAPL-OCT-2020-110-C" -> "AAPL"
     */
    private String extractUnderlyingTicker(String optionTicker) {
        // Option tickers typically follow pattern: STOCK-EXPIRY-STRIKE-TYPE
        // We need to extract the stock part before the first dash
        int firstDashIndex = optionTicker.indexOf('-');
        if (firstDashIndex > 0) {
            return optionTicker.substring(0, firstDashIndex);
        }
        return optionTicker; // Fallback to original if no dash found
    }
    
    /**
     * Simulates market data update and recalculates portfolio
     */
    public void updateMarketDataAndRecalculate(Portfolio portfolio) {
        // Update stock prices using geometric Brownian motion
        List<Position> positions = portfolio.getPositions();
        for (Position position : positions) {
            Security security = position.getSecurity();
            if (security != null && security.getType() == SecurityType.STOCK) {
                marketDataService.simulateNextPrice(security.getTicker(), security);
            }
        }
        
        // Recalculate all position values
        calculatePortfolioValues(portfolio);
    }
    
    /**
     * Gets a summary of the portfolio
     */
    public String getPortfolioSummary(Portfolio portfolio) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Portfolio Summary ===\n");
        summary.append("Total Positions: ").append(portfolio.getPositionCount()).append("\n");
        summary.append("Total NAV: $").append(portfolio.getTotalNAV().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
        summary.append("Last Updated: ").append(portfolio.getLastUpdated()).append("\n\n");
        
        summary.append("=== Position Details ===\n");
        for (Position position : portfolio.getPositions()) {
            summary.append(String.format("%-20s | %10s | $%10s | $%12s\n",
                    position.getSymbol(),
                    position.getPositionSize().setScale(0, BigDecimal.ROUND_HALF_UP),
                    position.getCurrentPrice().setScale(2, BigDecimal.ROUND_HALF_UP),
                    position.getMarketValue().setScale(2, BigDecimal.ROUND_HALF_UP)));
        }
        
        return summary.toString();
    }
}
