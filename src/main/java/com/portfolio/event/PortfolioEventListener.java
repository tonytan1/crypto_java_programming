package com.portfolio.event;

import com.portfolio.events.PortfolioEventProtos;

/**
 * Interface for listening to portfolio events.
 * Implement this interface to receive real-time events from the portfolio system.
 */
public interface PortfolioEventListener {
    
    /**
     * Called when a portfolio event is published.
     * 
     * @param event The portfolio event
     */
    void onEvent(PortfolioEventProtos.PortfolioEvent event);
    
    /**
     * Gets the listener name for logging purposes.
     * 
     * @return Listener name
     */
    default String getListenerName() {
        return this.getClass().getSimpleName();
    }
}
