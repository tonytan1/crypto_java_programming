package com.portfolio.event.listener;

import com.portfolio.event.PortfolioEventListener;
import com.portfolio.events.PortfolioEventProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event listener that logs portfolio events to the console.
 * This demonstrates how to consume real-time events from the portfolio system.
 */
@Component
public class ConsoleEventListener implements PortfolioEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsoleEventListener.class);
    
    @Override
    public void onEvent(PortfolioEventProtos.PortfolioEvent event) {
        // Use Switch Expression for cleaner event handling
        switch (event.getType()) {
            case MARKET_DATA_UPDATE -> handleMarketDataUpdate(event);
            case PORTFOLIO_RECALCULATED -> handlePortfolioRecalculated(event);
            case POSITION_ADDED, POSITION_REMOVED, POSITION_UPDATED -> handlePositionUpdate(event);
            case SYSTEM_STARTED -> handleSystemStarted(event);
            case SYSTEM_STOPPED -> handleSystemStopped(event);
            case ERROR_OCCURRED -> handleError(event);
            case PERFORMANCE_METRIC -> handlePerformanceMetric(event);
            default -> logger.debug("Unhandled event type: {}", event.getType());
        }
    }
    
    private void handleMarketDataUpdate(PortfolioEventProtos.PortfolioEvent event) {
        PortfolioEventProtos.MarketDataUpdate marketData = event.getMarketData();
        logger.debug("MARKET DATA: {} = ${} [{} ${} ({}%)]", 
                   marketData.getTicker(),
                   String.format("%.2f", marketData.getPrice()),
                   marketData.getDirection().name(),
                   String.format("%.2f", marketData.getAbsoluteChange()),
                   String.format("%.2f", marketData.getPercentageChange()));
    }
    
    private void handlePortfolioRecalculated(PortfolioEventProtos.PortfolioEvent event) {
        PortfolioEventProtos.PortfolioSummary summary = event.getPortfolioSummary();
        logger.info("PORTFOLIO: NAV = ${} | Positions = {} | Time = {}ms", 
                   String.format("%.2f", summary.getTotalNav()),
                   summary.getPositionCount(),
                   summary.getPerformance().getCalculationTimeMs());
    }
    
    private void handlePositionUpdate(PortfolioEventProtos.PortfolioEvent event) {
        PortfolioEventProtos.PositionUpdate positionUpdate = event.getPositionUpdate();
        logger.debug("POSITION: {} {} | Size: {} -> {} | Reason: {}", 
                   positionUpdate.getAction().name(),
                   positionUpdate.getSymbol(),
                   positionUpdate.getOldSize(),
                   positionUpdate.getNewSize(),
                   positionUpdate.getReason());
    }
    
    private void handleSystemStarted(PortfolioEventProtos.PortfolioEvent event) {
        logger.debug("SYSTEM: Portfolio system started at {}", event.getTimestamp());
    }
    
    private void handleSystemStopped(PortfolioEventProtos.PortfolioEvent event) {
        logger.debug("SYSTEM: Portfolio system stopped at {}", event.getTimestamp());
    }
    
    private void handleError(PortfolioEventProtos.PortfolioEvent event) {
        PortfolioEventProtos.SystemAlert alert = event.getSystemAlert();
        logger.error("ERROR: {} - {} | Component: {}", 
                    alert.getLevel().name(),
                    alert.getMessage(),
                    alert.getComponent());
    }
    
    private void handlePerformanceMetric(PortfolioEventProtos.PortfolioEvent event) {
        PortfolioEventProtos.PortfolioSummary summary = event.getPortfolioSummary();
        PortfolioEventProtos.PerformanceMetrics perf = summary.getPerformance();
        logger.debug("PERFORMANCE: Daily P&L = ${} | Volatility = {}% | Sharpe = {}", 
                   String.format("%.2f", perf.getDailyPnl()),
                   String.format("%.2f", perf.getPortfolioVolatility()),
                   String.format("%.2f", perf.getSharpeRatio()));
    }
}
