package com.portfolio.event;

import com.portfolio.events.PortfolioEventProtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Utility class for publishing portfolio events.
 * Provides convenient methods for creating and publishing different types of events.
 */
@Component
public class EventPublisher {
    
    
    @Autowired
    private EventBus eventBus;
    
    /**
     * Publishes a market data update event.
     */
    public void publishMarketDataUpdate(String ticker, BigDecimal currentPrice, BigDecimal previousPrice) {
        PortfolioEventProtos.MarketDataUpdate.Builder marketDataBuilder = 
            PortfolioEventProtos.MarketDataUpdate.newBuilder()
                .setTicker(ticker)
                .setPrice(currentPrice.doubleValue())
                .setPreviousPrice(previousPrice != null ? previousPrice.doubleValue() : 0.0);
        
        // Calculate changes
        if (previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal absoluteChange = currentPrice.subtract(previousPrice);
            BigDecimal percentageChange = absoluteChange.divide(previousPrice, 6, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
            
            marketDataBuilder
                .setAbsoluteChange(absoluteChange.doubleValue())
                .setPercentageChange(percentageChange.doubleValue())
                .setDirection(calculateDirection(currentPrice, previousPrice));
        } else {
            marketDataBuilder
                .setAbsoluteChange(0.0)
                .setPercentageChange(0.0)
                .setDirection(PortfolioEventProtos.ChangeDirection.NEW);
        }
        
        marketDataBuilder.setUpdateTimestamp(System.currentTimeMillis());
        
        PortfolioEventProtos.PortfolioEvent event = PortfolioEventProtos.PortfolioEvent.newBuilder()
            .setType(PortfolioEventProtos.EventType.MARKET_DATA_UPDATE)
            .setTimestamp(System.currentTimeMillis())
            .setSource("MarketDataService")
            .setMarketData(marketDataBuilder.build())
            .build();
        
        eventBus.publishEvent(event);
    }
    
    /**
     * Publishes a portfolio recalculation event.
     */
    public void publishPortfolioRecalculated(BigDecimal totalNav, int positionCount, long calculationTimeMs) {
        PortfolioEventProtos.PortfolioSummary.Builder summaryBuilder = 
            PortfolioEventProtos.PortfolioSummary.newBuilder()
                .setTotalNav(totalNav.doubleValue())
                .setPositionCount(positionCount)
                .setCalculationTimestamp(System.currentTimeMillis())
                .setPerformance(PortfolioEventProtos.PerformanceMetrics.newBuilder()
                    .setCalculationTimeMs(calculationTimeMs)
                    .build());
        
        PortfolioEventProtos.PortfolioEvent event = PortfolioEventProtos.PortfolioEvent.newBuilder()
            .setType(PortfolioEventProtos.EventType.PORTFOLIO_RECALCULATED)
            .setTimestamp(System.currentTimeMillis())
            .setSource("PortfolioCalculationService")
            .setPortfolioSummary(summaryBuilder.build())
            .build();
        
        eventBus.publishEvent(event);
    }
    
    /**
     * Publishes a position update event.
     */
    public void publishPositionUpdate(String symbol, BigDecimal oldSize, BigDecimal newSize, 
                                    PortfolioEventProtos.UpdateAction action, String reason) {
        PortfolioEventProtos.PositionUpdate positionUpdate = 
            PortfolioEventProtos.PositionUpdate.newBuilder()
                .setSymbol(symbol)
                .setOldSize(oldSize.doubleValue())
                .setNewSize(newSize.doubleValue())
                .setAction(action)
                .setReason(reason)
                .build();
        
        PortfolioEventProtos.EventType eventType;
        switch (action) {
            case ADDED:
                eventType = PortfolioEventProtos.EventType.POSITION_ADDED;
                break;
            case REMOVED:
                eventType = PortfolioEventProtos.EventType.POSITION_REMOVED;
                break;
            case SIZE_CHANGED:
            case PRICE_UPDATED:
                eventType = PortfolioEventProtos.EventType.POSITION_UPDATED;
                break;
            default:
                eventType = PortfolioEventProtos.EventType.POSITION_UPDATED;
                break;
        }
        
        PortfolioEventProtos.PortfolioEvent event = PortfolioEventProtos.PortfolioEvent.newBuilder()
            .setType(eventType)
            .setTimestamp(System.currentTimeMillis())
            .setSource("PortfolioManagerService")
            .setPositionUpdate(positionUpdate)
            .build();
        
        eventBus.publishEvent(event);
    }
    
    /**
     * Publishes a system alert event.
     */
    public void publishSystemAlert(PortfolioEventProtos.AlertLevel level, String message, 
                                  String component, Map<String, String> metadata) {
        PortfolioEventProtos.SystemAlert.Builder alertBuilder = 
            PortfolioEventProtos.SystemAlert.newBuilder()
                .setLevel(level)
                .setMessage(message)
                .setComponent(component);
        
        if (metadata != null) {
            alertBuilder.putAllMetadata(metadata);
        }
        
        PortfolioEventProtos.PortfolioEvent event = PortfolioEventProtos.PortfolioEvent.newBuilder()
            .setType(PortfolioEventProtos.EventType.ERROR_OCCURRED)
            .setTimestamp(System.currentTimeMillis())
            .setSource(component)
            .setSystemAlert(alertBuilder.build())
            .build();
        
        eventBus.publishEvent(event);
    }
    
    /**
     * Publishes a system started event.
     */
    public void publishSystemStarted() {
        PortfolioEventProtos.PortfolioEvent event = PortfolioEventProtos.PortfolioEvent.newBuilder()
            .setType(PortfolioEventProtos.EventType.SYSTEM_STARTED)
            .setTimestamp(System.currentTimeMillis())
            .setSource("PortfolioApplication")
            .build();
        
        eventBus.publishEvent(event);
    }
    
    /**
     * Publishes a system stopped event.
     */
    public void publishSystemStopped() {
        PortfolioEventProtos.PortfolioEvent event = PortfolioEventProtos.PortfolioEvent.newBuilder()
            .setType(PortfolioEventProtos.EventType.SYSTEM_STOPPED)
            .setTimestamp(System.currentTimeMillis())
            .setSource("PortfolioApplication")
            .build();
        
        eventBus.publishEvent(event);
    }
    
    /**
     * Calculates the direction of price change.
     */
    private PortfolioEventProtos.ChangeDirection calculateDirection(BigDecimal current, BigDecimal previous) {
        if (previous == null) {
            return PortfolioEventProtos.ChangeDirection.NEW;
        }
        
        int comparison = current.compareTo(previous);
        if (comparison > 0) {
            return PortfolioEventProtos.ChangeDirection.UP;
        } else if (comparison < 0) {
            return PortfolioEventProtos.ChangeDirection.DOWN;
        } else {
            return PortfolioEventProtos.ChangeDirection.SAME;
        }
    }
}
