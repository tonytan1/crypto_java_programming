package com.portfolio.event;

import com.portfolio.events.PortfolioEventProtos;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Simple in-memory event bus for portfolio events.
 * In a production system, this would be replaced with a proper message broker like Kafka, RabbitMQ, or Redis.
 */
@Component
public class EventBus {
    
    private static final Logger logger = Logger.getLogger(EventBus.class.getName());
    
    private final List<PortfolioEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    @Value("${portfolio.eventbus.thread-pool.core-pool-size:2}")
    private int corePoolSize;
    
    @Value("${portfolio.eventbus.thread-pool.max-pool-size:8}")
    private int maxPoolSize;
    
    @Value("${portfolio.eventbus.thread-pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${portfolio.eventbus.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;
    
    @Value("${portfolio.eventbus.thread-pool.thread-name-prefix:event-bus-}")
    private String threadNamePrefix;
    
    @Value("${portfolio.eventbus.thread-pool.wait-for-tasks-to-complete-on-shutdown:true}")
    private boolean waitForTasksToCompleteOnShutdown;
    
    @Value("${portfolio.eventbus.thread-pool.await-termination-seconds:30}")
    private int awaitTerminationSeconds;
    
    @Value("${portfolio.eventbus.thread-pool.rejection-policy:CALLER_RUNS}")
    private String rejectionPolicy;
    
    private ThreadPoolTaskExecutor executor;
    
    /**
     * Initializes the thread pool executor after Spring dependency injection.
     */
    @PostConstruct
    public void initializeExecutor() {
        this.executor = createEventBusExecutor();
    }
    
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
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            logger.info("Registered event listener: " + listener.getClass().getSimpleName());
        } else {
            logger.fine("Event listener already registered: " + listener.getClass().getSimpleName());
        }
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
     * Creates a ThreadPoolTaskExecutor configured via YAML properties.
     */
    private ThreadPoolTaskExecutor createEventBusExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        executor.setRejectedExecutionHandler(createRejectionPolicy(rejectionPolicy));
        
        executor.initialize();
        
        logger.info(String.format("EventBus thread pool initialized: core=%d, max=%d, queue=%d, policy=%s", 
            corePoolSize, maxPoolSize, queueCapacity, rejectionPolicy));
        
        return executor;
    }
    
    /**
     * Creates rejection policy based on configuration string.
     */
    private java.util.concurrent.RejectedExecutionHandler createRejectionPolicy(String policy) {
        switch (policy.toUpperCase()) {
            case "CALLER_RUNS":
                return new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy();
            case "ABORT":
                return new java.util.concurrent.ThreadPoolExecutor.AbortPolicy();
            case "DISCARD":
                return new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST":
                return new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                logger.warning("Unknown rejection policy: " + policy + ", using CALLER_RUNS");
                return new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy();
        }
    }
    
    /**
     * Shuts down the event bus and its executor.
     */
    public void shutdown() {
        logger.info("Shutting down event bus...");
        executor.shutdown();
    }
}
