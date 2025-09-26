package com.portfolio.service;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for simulating market data using geometric Brownian motion.
 * Generates random stock prices that follow the discrete-time geometric Brownian motion model.
 */
@Service
public class MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    
    @Autowired
    private SecurityRepository securityRepository;
    
    @Value("${portfolio.marketdata.update-interval-min:500}")
    private long minUpdateInterval;
    
    @Value("${portfolio.marketdata.update-interval-max:2000}")
    private long maxUpdateInterval;
    
    private final Map<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> initialPrices = new HashMap<>();
    private final Random random = new Random();
    
    @PostConstruct
    public void initializePrices() {
        logger.info("Initializing market data service...");
        List<Security> stocks = securityRepository.findByType(SecurityType.STOCK);
        
        for (Security stock : stocks) {
            // Set initial prices (you can customize these)
            BigDecimal initialPrice = getInitialPrice(stock.getTicker());
            initialPrices.put(stock.getTicker(), initialPrice);
            currentPrices.put(stock.getTicker(), initialPrice);
            logger.info("Initialized {} with price: ${}", stock.getTicker(), initialPrice);
        }
        
        logger.info("Market data service initialized with {} stocks", stocks.size());
    }
    
    /**
     * Gets the current price of a security
     */
    public BigDecimal getCurrentPrice(String ticker) {
        return currentPrices.getOrDefault(ticker, BigDecimal.ZERO);
    }
    
    /**
     * Gets all current prices
     */
    public Map<String, BigDecimal> getAllCurrentPrices() {
        return new HashMap<>(currentPrices);
    }
    
    /**
     * Simulates the next price for a stock using geometric Brownian motion
     */
    public BigDecimal simulateNextPrice(String ticker, Security stock) {
        BigDecimal currentPrice = currentPrices.get(ticker);
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Geometric Brownian motion parameters
        BigDecimal mu = stock.getMu(); // Expected return
        BigDecimal sigma = stock.getSigma(); // Volatility
        
        // Random time interval between 0.5-2 seconds (converted to years)
        double deltaT = (minUpdateInterval + random.nextDouble() * (maxUpdateInterval - minUpdateInterval)) / 1000.0 / 365.0;
        
        // Generate random normal variable (Box-Muller transformation)
        double epsilon = generateNormalRandom();
        
        // Calculate price change: deltaS = mu * S * deltaT + sigma * S * sqrt(deltaT) * epsilon
        BigDecimal muTerm = mu.multiply(currentPrice).multiply(new BigDecimal(deltaT));
        BigDecimal sigmaTerm = sigma.multiply(currentPrice)
                .multiply(new BigDecimal(Math.sqrt(deltaT)))
                .multiply(new BigDecimal(epsilon));
        
        BigDecimal deltaS = muTerm.add(sigmaTerm);
        BigDecimal newPrice = currentPrice.add(deltaS);
        
        // Ensure price never goes below zero
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            newPrice = BigDecimal.ZERO;
        }
        
        currentPrices.put(ticker, newPrice);
        return newPrice;
    }
    
    /**
     * Generates a random number from standard normal distribution using Box-Muller transformation
     */
    private double generateNormalRandom() {
        // Box-Muller transformation
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        
        double z0 = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        return z0;
    }
    
    /**
     * Gets initial price for a stock (customize as needed)
     */
    private BigDecimal getInitialPrice(String ticker) {
        // You can customize initial prices here
        switch (ticker.toUpperCase()) {
            case "AAPL":
                return new BigDecimal("150.00");
            case "TELSA":
                return new BigDecimal("800.00");
            default:
                return new BigDecimal("100.00");
        }
    }
    
    /**
     * Resets prices to initial values
     */
    public void resetPrices() {
        for (Map.Entry<String, BigDecimal> entry : initialPrices.entrySet()) {
            currentPrices.put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Gets the price update interval range
     */
    public long getMinUpdateInterval() {
        return minUpdateInterval;
    }
    
    public long getMaxUpdateInterval() {
        return maxUpdateInterval;
    }
}
