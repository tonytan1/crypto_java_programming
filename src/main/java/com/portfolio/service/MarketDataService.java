package com.portfolio.service;

import com.portfolio.event.EventPublisher;
import com.portfolio.marketdata.MarketDataProtos;
import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import com.portfolio.repository.SecurityRepository;
import com.portfolio.util.ProtobufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for simulating market data using geometric Brownian motion.
 * Generates random stock prices that follow the discrete-time geometric Brownian motion model.
 */
@Service
public class MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    
    @Autowired
    private SecurityRepository securityRepository;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Value("${portfolio.marketdata.update-interval-min:500}")
    private long minUpdateInterval;
    
    @Value("${portfolio.marketdata.update-interval-max:2000}")
    private long maxUpdateInterval;
    
    private final Map<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> initialPrices = new ConcurrentHashMap<>();
    
    // Independent random number generators for each stock to avoid correlation
    private final Map<String, AtomicLong> randomSeeds = new ConcurrentHashMap<>();
    
    public void initializePrices() {
        logger.info("Initializing market data service...");
        List<Security> stocks = securityRepository.findByType(SecurityType.STOCK);
        
        for (Security stock : stocks) {
            // Set initial prices (you can customize these)
            BigDecimal initialPrice = getInitialPrice(stock.getTicker());
            initialPrices.put(stock.getTicker(), initialPrice);
            currentPrices.put(stock.getTicker(), initialPrice);
            
            // Initialize independent random seed for each stock
            randomSeeds.put(stock.getTicker(), new AtomicLong(System.nanoTime() + stock.getTicker().hashCode()));
            
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

        // Store previous price for event publishing
        BigDecimal previousPrice = currentPrice;

        // Geometric Brownian motion parameters
        BigDecimal mu = stock.getMu(); // Expected return
        BigDecimal sigma = stock.getSigma(); // Volatility

        // Random time interval between 0.5-2 seconds (converted to years)
        double deltaT = (minUpdateInterval + generateUniformRandom(ticker) * (maxUpdateInterval - minUpdateInterval)) / 1000.0 / 365.0;

        // Generate random normal variable (Box-Muller transformation)
        double epsilon = generateNormalRandom(ticker);

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

        // Update price
        currentPrices.put(ticker, newPrice);
        
        // Publish market data update event
        eventPublisher.publishMarketDataUpdate(ticker, newPrice, previousPrice);
        
        return newPrice;
    }
    
    /**
     * Generates a uniform random number in range [0, 1) using Linear Congruential Generator
     * Thread-safe implementation using AtomicLong for a specific ticker
     */
    private double generateUniformRandom(String ticker) {
        AtomicLong seed = randomSeeds.get(ticker);
        if (seed == null) {
            // Fallback: create a new seed if ticker not found
            seed = new AtomicLong(System.nanoTime() + ticker.hashCode());
            randomSeeds.put(ticker, seed);
        }
        long nextSeed = seed.updateAndGet(s -> (s * 1103515245L + 12345L) & 0x7fffffffL);
        return nextSeed / (double) 0x80000000L;
    }
    
    /**
     * Generates a random number from standard normal distribution using Box-Muller transformation
     * Thread-safe implementation using AtomicLong + LCG for a specific ticker
     */
    private double generateNormalRandom(String ticker) {
        // Generate two uniform random numbers using thread-safe LCG
        double u1 = generateUniformRandom(ticker);
        double u2 = generateUniformRandom(ticker);
        
        // Box-Muller transformation
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
    
    /**
     * Creates a MarketDataSnapshot Protobuf message with current prices and change information.
     * 
     * @param previousPrices Map of previous prices for change calculation
     * @return MarketDataSnapshot Protobuf message
     */
    public MarketDataProtos.MarketDataSnapshot createMarketDataSnapshot(Map<String, BigDecimal> previousPrices) {
        return ProtobufUtils.createMarketDataSnapshot(currentPrices, previousPrices);
    }
    
    /**
     * Creates a MarketDataUpdate Protobuf message for a single security.
     * 
     * @param ticker Security ticker symbol
     * @param previousPrice Previous price for change calculation
     * @return MarketDataUpdate Protobuf message
     */
    public MarketDataProtos.MarketDataUpdate createMarketDataUpdate(String ticker, BigDecimal previousPrice) {
        BigDecimal currentPrice = currentPrices.get(ticker);
        if (currentPrice == null) {
            currentPrice = BigDecimal.ZERO;
        }
        
        long timestamp = System.currentTimeMillis();
        return ProtobufUtils.createMarketDataUpdate(ticker, currentPrice, previousPrice, timestamp);
    }
    
    /**
     * Serializes current market data to byte array using Protobuf.
     * 
     * @param previousPrices Map of previous prices for change calculation
     * @return Serialized byte array
     */
    public byte[] serializeMarketData(Map<String, BigDecimal> previousPrices) {
        MarketDataProtos.MarketDataSnapshot snapshot = createMarketDataSnapshot(previousPrices);
        return ProtobufUtils.serializeMarketDataSnapshot(snapshot);
    }
    
    /**
     * Logs market data in Protobuf format for debugging.
     * 
     * @param previousPrices Map of previous prices for change calculation
     */
    public void logMarketDataSnapshot(Map<String, BigDecimal> previousPrices) {
        MarketDataProtos.MarketDataSnapshot snapshot = createMarketDataSnapshot(previousPrices);
        logger.debug("Market Data Snapshot (Protobuf):\n{}", ProtobufUtils.toReadableString(snapshot));
    }
}
