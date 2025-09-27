package com.portfolio.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.portfolio.marketdata.MarketDataProtos;
import java.util.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

/**
 * Utility class for Protobuf serialization and deserialization operations.
 * Provides methods to convert between Java objects and Protobuf messages.
 */
public class ProtobufUtils {
    
    private static final Logger logger = Logger.getLogger(ProtobufUtils.class.getName());
    
    /**
     * Converts a Map of ticker->price to a MarketDataSnapshot Protobuf message.
     * 
     * @param prices Map of ticker symbols to current prices
     * @param previousPrices Map of ticker symbols to previous prices for change calculation
     * @return MarketDataSnapshot Protobuf message
     */
    public static MarketDataProtos.MarketDataSnapshot createMarketDataSnapshot(
            Map<String, BigDecimal> prices, 
            Map<String, BigDecimal> previousPrices) {
        
        MarketDataProtos.MarketDataSnapshot.Builder snapshotBuilder = 
            MarketDataProtos.MarketDataSnapshot.newBuilder();
        
        long currentTime = Instant.now().toEpochMilli();
        snapshotBuilder.setSnapshotTime(currentTime);
        snapshotBuilder.setTotalSecurities(prices.size());
        
        for (Map.Entry<String, BigDecimal> entry : prices.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal currentPrice = entry.getValue();
            BigDecimal previousPrice = previousPrices.get(ticker);
            
            MarketDataProtos.MarketDataUpdate update = createMarketDataUpdate(
                ticker, currentPrice, previousPrice, currentTime);
            
            snapshotBuilder.addUpdates(update);
        }
        
        return snapshotBuilder.build();
    }
    
    /**
     * Creates a MarketDataUpdate Protobuf message for a single security.
     * 
     * @param ticker Security ticker symbol
     * @param currentPrice Current price
     * @param previousPrice Previous price (can be null for new prices)
     * @param timestamp Timestamp in milliseconds
     * @return MarketDataUpdate Protobuf message
     */
    public static MarketDataProtos.MarketDataUpdate createMarketDataUpdate(
            String ticker, 
            BigDecimal currentPrice, 
            BigDecimal previousPrice, 
            long timestamp) {
        
        MarketDataProtos.MarketDataUpdate.Builder updateBuilder = 
            MarketDataProtos.MarketDataUpdate.newBuilder();
        
        updateBuilder.setTicker(ticker);
        updateBuilder.setPrice(currentPrice.doubleValue());
        updateBuilder.setTimestamp(timestamp);
        updateBuilder.setSource("PORTFOLIO_SIMULATOR");
        
        // Calculate price change
        MarketDataProtos.PriceChange priceChange = calculatePriceChange(currentPrice, previousPrice);
        updateBuilder.setPriceChange(priceChange);
        
        return updateBuilder.build();
    }
    
    /**
     * Calculates price change information for a security.
     * 
     * @param currentPrice Current price
     * @param previousPrice Previous price (can be null)
     * @return PriceChange Protobuf message
     */
    private static MarketDataProtos.PriceChange calculatePriceChange(
            BigDecimal currentPrice, 
            BigDecimal previousPrice) {
        
        MarketDataProtos.PriceChange.Builder changeBuilder = 
            MarketDataProtos.PriceChange.newBuilder();
        
        if (previousPrice == null) {
            // New price
            changeBuilder.setAbsoluteChange(0.0);
            changeBuilder.setPercentageChange(0.0);
            changeBuilder.setDirection(MarketDataProtos.ChangeDirection.NEW);
        } else {
            // Calculate changes
            BigDecimal absoluteChange = currentPrice.subtract(previousPrice);
            BigDecimal percentageChange = BigDecimal.ZERO;
            
            if (previousPrice.compareTo(BigDecimal.ZERO) != 0) {
                percentageChange = absoluteChange.divide(previousPrice, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            
            changeBuilder.setAbsoluteChange(absoluteChange.doubleValue());
            changeBuilder.setPercentageChange(percentageChange.doubleValue());
            
            // Determine direction
            int comparison = currentPrice.compareTo(previousPrice);
            if (comparison > 0) {
                changeBuilder.setDirection(MarketDataProtos.ChangeDirection.UP);
            } else if (comparison < 0) {
                changeBuilder.setDirection(MarketDataProtos.ChangeDirection.DOWN);
            } else {
                changeBuilder.setDirection(MarketDataProtos.ChangeDirection.SAME);
            }
        }
        
        return changeBuilder.build();
    }
    
    /**
     * Serializes a MarketDataSnapshot to byte array.
     * 
     * @param snapshot MarketDataSnapshot to serialize
     * @return Serialized byte array
     */
    public static byte[] serializeMarketDataSnapshot(MarketDataProtos.MarketDataSnapshot snapshot) {
        try {
            return snapshot.toByteArray();
        } catch (Exception e) {
            logger.severe("Failed to serialize MarketDataSnapshot: " + e.getMessage());
            throw new RuntimeException("Serialization failed", e);
        }
    }
    
    /**
     * Deserializes a byte array to MarketDataSnapshot.
     * 
     * @param data Serialized byte array
     * @return Deserialized MarketDataSnapshot
     */
    public static MarketDataProtos.MarketDataSnapshot deserializeMarketDataSnapshot(byte[] data) {
        try {
            return MarketDataProtos.MarketDataSnapshot.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.severe("Failed to deserialize MarketDataSnapshot: " + e.getMessage());
            throw new RuntimeException("Deserialization failed", e);
        }
    }
    
    /**
     * Serializes a MarketDataUpdate to byte array.
     * 
     * @param update MarketDataUpdate to serialize
     * @return Serialized byte array
     */
    public static byte[] serializeMarketDataUpdate(MarketDataProtos.MarketDataUpdate update) {
        try {
            return update.toByteArray();
        } catch (Exception e) {
            logger.severe("Failed to serialize MarketDataUpdate: " + e.getMessage());
            throw new RuntimeException("Serialization failed", e);
        }
    }
    
    /**
     * Deserializes a byte array to MarketDataUpdate.
     * 
     * @param data Serialized byte array
     * @return Deserialized MarketDataUpdate
     */
    public static MarketDataProtos.MarketDataUpdate deserializeMarketDataUpdate(byte[] data) {
        try {
            return MarketDataProtos.MarketDataUpdate.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.severe("Failed to deserialize MarketDataUpdate: " + e.getMessage());
            throw new RuntimeException("Deserialization failed", e);
        }
    }
    
    /**
     * Converts a MarketDataSnapshot to a human-readable string for logging.
     * 
     * @param snapshot MarketDataSnapshot to convert
     * @return Human-readable string representation
     */
    public static String toReadableString(MarketDataProtos.MarketDataSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("MarketDataSnapshot {\n");
        sb.append("  Snapshot Time: ").append(snapshot.getSnapshotTime()).append("\n");
        sb.append("  Total Securities: ").append(snapshot.getTotalSecurities()).append("\n");
        sb.append("  Updates:\n");
        
        for (MarketDataProtos.MarketDataUpdate update : snapshot.getUpdatesList()) {
            sb.append("    ").append(update.getTicker())
              .append(": $").append(String.format("%.2f", update.getPrice()));
            
            MarketDataProtos.PriceChange change = update.getPriceChange();
            if (change.getDirection() != MarketDataProtos.ChangeDirection.SAME) {
                sb.append(" [").append(change.getDirection().name())
                  .append(" $").append(String.format("%.2f", change.getAbsoluteChange()))
                  .append(" (").append(String.format("%.2f", change.getPercentageChange()))
                  .append("%)]");
            }
            sb.append("\n");
        }
        sb.append("}");
        
        return sb.toString();
    }
}
