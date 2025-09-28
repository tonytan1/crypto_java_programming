package com.portfolio.event;

import com.portfolio.events.PortfolioEventProtos;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory event bus for portfolio events.
 * In a production system, this would be replaced with a proper message broker like Kafka, RabbitMQ, or Redis.
 */
@Component
public class EventBus {
    
    private static final Logger logger = Logger.getLogger(EventBus.class.getName());
    
    private final List<PortfolioEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    /**
     * Publishes an event to all registered listeners asynchronously.
     * 
     * @param event The event to publish
     */
    public void publishEvent(PortfolioEventProtos.PortfolioEvent event) {
        if (listeners.isEmpty()) {
            logger.fine("No listeners registered for event: " + event.getType());
            return;
        }
        
        // Add event ID and timestamp if not set
        PortfolioEventProtos.PortfolioEvent.Builder eventBuilder = event.toBuilder();
        if (event.getEventId().isEmpty()) {
            eventBuilder.setEventId("event_" + eventCounter.incrementAndGet());
        }
        if (event.getTimestamp() == 0) {
            eventBuilder.setTimestamp(System.currentTimeMillis());
        }
        
        PortfolioEventProtos.PortfolioEvent finalEvent = eventBuilder.build();
        
        // Publish to all listeners asynchronously
        for (PortfolioEventListener listener : listeners) {
            executor.submit(() -> {
                try {
                    listener.onEvent(finalEvent);
                } catch (Exception e) {
                    logger.severe(String.format("Error delivering event to listener %s: %s", 
                        listener.getClass().getSimpleName(), e.getMessage()));
                }
            });
        }
        
        logger.fine(String.format("Published event %s to %d listeners", finalEvent.getType(), listeners.size()));
    }
    
    /**
     * Registers a new event listener.
     * 
     * @param listener The listener to register
     */
    public void subscribe(PortfolioEventListener listener) {
        listeners.add(listener);
        logger.info("Registered event listener: " + listener.getClass().getSimpleName());
    }
    
    /**
     * Unregisters an event listener.
     * 
     * @param listener The listener to unregister
     */
    public void unsubscribe(PortfolioEventListener listener) {
        listeners.remove(listener);
        logger.info("Unregistered event listener: " + listener.getClass().getSimpleName());
    }
    
    /**
     * Gets the number of registered listeners.
     * 
     * @return Number of listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * Shuts down the event bus and its executor.
     */
    public void shutdown() {
        logger.info("Shutting down event bus...");
        executor.shutdown();
    }
}
