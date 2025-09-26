package com.portfolio;

import com.portfolio.marketdata.MarketDataProtos;
import com.portfolio.util.ProtobufUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Protobuf market data functionality.
 */
public class ProtobufIntegrationTest {

    @Test
    public void testMarketDataSnapshotCreation() {
        // Create test data
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        currentPrices.put("AAPL", new BigDecimal("150.00"));
        currentPrices.put("TELSA", new BigDecimal("800.00"));
        
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("148.50"));
        previousPrices.put("TELSA", new BigDecimal("795.00"));
        
        // Create Protobuf snapshot
        MarketDataProtos.MarketDataSnapshot snapshot = 
            ProtobufUtils.createMarketDataSnapshot(currentPrices, previousPrices);
        
        // Verify snapshot properties
        assertEquals(2, snapshot.getTotalSecurities());
        assertEquals(2, snapshot.getUpdatesCount());
        assertTrue(snapshot.getSnapshotTime() > 0);
        
        // Verify individual updates
        for (MarketDataProtos.MarketDataUpdate update : snapshot.getUpdatesList()) {
            assertNotNull(update.getTicker());
            assertTrue(update.getPrice() > 0);
            assertNotNull(update.getPriceChange());
            assertNotNull(update.getSource());
        }
    }
    
    @Test
    public void testPriceChangeCalculation() {
        // Test price increase
        BigDecimal currentPrice = new BigDecimal("150.00");
        BigDecimal previousPrice = new BigDecimal("148.50");
        
        MarketDataProtos.MarketDataUpdate update = 
            ProtobufUtils.createMarketDataUpdate("AAPL", currentPrice, previousPrice, System.currentTimeMillis());
        
        assertEquals("AAPL", update.getTicker());
        assertEquals(150.00, update.getPrice(), 0.01);
        assertEquals(MarketDataProtos.ChangeDirection.UP, update.getPriceChange().getDirection());
        assertEquals(1.50, update.getPriceChange().getAbsoluteChange(), 0.01);
        assertTrue(update.getPriceChange().getPercentageChange() > 0);
    }
    
    @Test
    public void testSerializationAndDeserialization() {
        // Create test data
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        currentPrices.put("AAPL", new BigDecimal("150.00"));
        
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("148.50"));
        
        // Create and serialize
        MarketDataProtos.MarketDataSnapshot originalSnapshot = 
            ProtobufUtils.createMarketDataSnapshot(currentPrices, previousPrices);
        byte[] serializedData = ProtobufUtils.serializeMarketDataSnapshot(originalSnapshot);
        
        // Deserialize
        MarketDataProtos.MarketDataSnapshot deserializedSnapshot = 
            ProtobufUtils.deserializeMarketDataSnapshot(serializedData);
        
        // Verify data integrity
        assertEquals(originalSnapshot.getTotalSecurities(), deserializedSnapshot.getTotalSecurities());
        assertEquals(originalSnapshot.getSnapshotTime(), deserializedSnapshot.getSnapshotTime());
        assertEquals(originalSnapshot.getUpdatesCount(), deserializedSnapshot.getUpdatesCount());
        
        // Verify individual updates
        MarketDataProtos.MarketDataUpdate originalUpdate = originalSnapshot.getUpdates(0);
        MarketDataProtos.MarketDataUpdate deserializedUpdate = deserializedSnapshot.getUpdates(0);
        
        assertEquals(originalUpdate.getTicker(), deserializedUpdate.getTicker());
        assertEquals(originalUpdate.getPrice(), deserializedUpdate.getPrice(), 0.01);
        assertEquals(originalUpdate.getPriceChange().getDirection(), 
                    deserializedUpdate.getPriceChange().getDirection());
    }
    
    @Test
    public void testNewPriceHandling() {
        // Test new price (no previous price)
        BigDecimal currentPrice = new BigDecimal("150.00");
        
        MarketDataProtos.MarketDataUpdate update = 
            ProtobufUtils.createMarketDataUpdate("AAPL", currentPrice, null, System.currentTimeMillis());
        
        assertEquals("AAPL", update.getTicker());
        assertEquals(150.00, update.getPrice(), 0.01);
        assertEquals(MarketDataProtos.ChangeDirection.NEW, update.getPriceChange().getDirection());
        assertEquals(0.0, update.getPriceChange().getAbsoluteChange(), 0.01);
        assertEquals(0.0, update.getPriceChange().getPercentageChange(), 0.01);
    }
    
    @Test
    public void testReadableStringFormat() {
        // Create test data
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        currentPrices.put("AAPL", new BigDecimal("150.00"));
        
        Map<String, BigDecimal> previousPrices = new HashMap<>();
        previousPrices.put("AAPL", new BigDecimal("148.50"));
        
        // Create snapshot and convert to readable string
        MarketDataProtos.MarketDataSnapshot snapshot = 
            ProtobufUtils.createMarketDataSnapshot(currentPrices, previousPrices);
        String readableString = ProtobufUtils.toReadableString(snapshot);
        
        // Verify string contains expected information
        assertTrue(readableString.contains("MarketDataSnapshot"));
        assertTrue(readableString.contains("AAPL"));
        assertTrue(readableString.contains("150.00"));
        assertTrue(readableString.contains("UP"));
    }
}
