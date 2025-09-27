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
    
    @Value("#{${portfolio.marketdata.initial-prices:{'AAPL':'150.00','TELSA':'800.00'}}}")
    private Map<String, String> initialPricesConfig;
    
    private final Map<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> initialPrices = new ConcurrentHashMap<>();
    
    private final Map<String, AtomicLong> randomSeeds = new ConcurrentHashMap<>();
    
    public void initializePrices() {
        logger.info("Initializing market data service...");
        logger.info("Using initial prices configuration: {}", initialPricesConfig);
        
        List<Security> stocks = securityRepository.findByType(SecurityType.STOCK);
        if (stocks.isEmpty()) {
            logger.warn("No stocks found in database");
            return;
        }
        
        for (Security stock : stocks) {
            try {
                BigDecimal initialPrice = getInitialPrice(stock.getTicker());
                if (initialPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    logger.warn("Invalid initial price for {}: {}", stock.getTicker(), initialPrice);
                    continue;
                }
                
                initialPrices.put(stock.getTicker(), initialPrice);
                currentPrices.put(stock.getTicker(), initialPrice);
                randomSeeds.put(stock.getTicker(), new AtomicLong(System.nanoTime() + stock.getTicker().hashCode()));
                
                logger.info("Initialized {} with price: ${}", stock.getTicker(), initialPrice);
            } catch (Exception e) {
                logger.error("Error initializing price for {}: {}", stock.getTicker(), e.getMessage(), e);
            }
        }
        
        logger.info("Market data service initialized with {} stocks", stocks.size());
    }
    
    public BigDecimal getCurrentPrice(String ticker) {
        return currentPrices.getOrDefault(ticker, BigDecimal.ZERO);
    }
    
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

        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            newPrice = BigDecimal.ZERO;
        }

        currentPrices.put(ticker, newPrice);
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
     * Gets initial price for a stock from configuration
     */
    private BigDecimal getInitialPrice(String ticker) {
        try {
            // Get price directly from the configured map
            String priceStr = initialPricesConfig.get(ticker.toUpperCase());
            
            if (priceStr != null) {
                return new BigDecimal(priceStr);
            } else {
                // Return default price if not configured
                logger.debug("No initial price configured for {}, using default $100.00", ticker);
                return new BigDecimal("100.00");
            }
        } catch (NumberFormatException e) {
            String priceStr = initialPricesConfig.get(ticker.toUpperCase());
            logger.warn("Invalid price format for {}: {}, using default", ticker, priceStr, e.getMessage());
            return new BigDecimal("100.00");
        } catch (Exception e) {
            logger.warn("Error getting initial price for {}, using default: {}", ticker, e.getMessage());
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
