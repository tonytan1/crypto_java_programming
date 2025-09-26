# Real-Time Event Streaming with Protobuf

## 🚀 **Overview**

Real-time event streaming is a powerful architectural pattern that enables **decoupled, scalable, and real-time communication** between different parts of the portfolio system. Instead of direct method calls, the system publishes events that multiple subscribers can listen to and process independently.

## 🏗️ **Architecture Comparison**

### **Before: Direct Method Calls**
```
PortfolioManagerService → MarketDataService → PortfolioCalculationService
                    ↓
              Console Output (Single Consumer)
```

**Problems:**
- Tight coupling between services
- Hard to add new consumers
- Difficult to scale
- No real-time capabilities for external systems

### **After: Event Streaming**
```
MarketDataService → Event Bus → Multiple Subscribers
                    ↓
    Console, Web UI, Database, External APIs, Mobile Apps, etc.
```

**Benefits:**
- ✅ **Decoupled**: Services don't know about each other
- ✅ **Scalable**: Easy to add new subscribers
- ✅ **Real-time**: Events delivered immediately
- ✅ **Flexible**: Each subscriber processes events differently
- ✅ **Observable**: Easy to monitor system behavior

## 📋 **Event Types**

### **1. Market Data Events**
```protobuf
message MarketDataUpdate {
  string ticker = 1;           // "AAPL", "TELSA"
  double price = 2;            // Current price
  int64 timestamp = 3;         // Unix timestamp in milliseconds
  PriceChange price_change = 4; // Price change information
}

message PriceChange {
  double absolute_change = 1;  // Absolute price change
  double percentage_change = 2; // Percentage change
  ChangeDirection direction = 3; // Direction of change
}

enum ChangeDirection { UP = 0; DOWN = 1; SAME = 2; NEW = 3; }
```

**When Published:** Every time a stock price changes
**Subscribers:** Console logger, Database logger, Risk management system

### **2. Portfolio Events**
```protobuf
message PortfolioSummary {
  double total_nav = 1;        // $1,234,567.89
  int32 position_count = 2;    // 10 positions
  int64 calculation_timestamp = 3;
  PerformanceMetrics performance = 4;
}

message PerformanceMetrics {
  double daily_pnl = 1;        // Daily profit/loss
  double portfolio_volatility = 2; // Portfolio volatility
  double sharpe_ratio = 3;     // Risk-adjusted return
  int64 calculation_time_ms = 4; // Calculation time
}
```

**When Published:** After portfolio recalculation
**Subscribers:** Console display, Performance analytics, Risk monitoring

### **3. Position Events**
```protobuf
message PositionUpdate {
  string symbol = 1;           // "AAPL"
  double old_size = 2;         // 1000 shares
  double new_size = 3;         // 1500 shares
  UpdateAction action = 4;     // ADDED, REMOVED, UPDATED
  string reason = 5;           // "Initial portfolio load", "User trade"
}

enum UpdateAction {
  ADDED = 0;
  REMOVED = 1;
  UPDATED = 2;
}
```

**When Published:** When positions are added, removed, or modified
**Subscribers:** Trade logging, Compliance system, Risk management, Audit trail

### **4. System Events**
```protobuf
message SystemAlert {
  AlertLevel level = 1;        // INFO, WARNING, ERROR, CRITICAL
  string message = 2;          // "High volatility detected"
  string component = 3;        // "RiskEngine"
  map<string, string> metadata = 4; // Additional context
}

enum AlertLevel {
  INFO = 0;
  WARNING = 1;
  ERROR = 2;
  CRITICAL = 3;
}
```

**When Published:** System errors, warnings, or important state changes
**Subscribers:** Monitoring dashboard, Alerting system, Log aggregation

## 🔧 **Implementation Components**

### **1. Event Bus (`EventBus.java`)**
- **Purpose**: Central hub for event distribution
- **Features**: Asynchronous delivery, error handling, listener management
- **Production**: Would be replaced with Kafka, RabbitMQ, or Redis Streams

### **2. Event Listeners**
- **`ConsoleEventListener`**: Logs events to console with smart logging levels
- **`DatabaseEventListener`**: Stores events in database for historical analysis (future)
- **`RiskEventListener`**: Monitors for risk threshold breaches (future)

### **3. Event Publisher (`EventPublisher.java`)**
- **Purpose**: Convenient methods for creating and publishing events
- **Features**: Type-safe event creation, automatic field population
- **Usage**: Services call publisher methods instead of creating events manually

## 🎯 **Real-World Use Cases**

### **1. Console Display (Current Implementation)**
```java
// ConsoleEventListener receives real-time updates
@Override
public void onEvent(PortfolioEventProtos.PortfolioEvent event) {
    switch (event.getType()) {
        case MARKET_DATA_UPDATE:
            handleMarketDataUpdate(event);
            break;
        case PORTFOLIO_RECALCULATED:
            handlePortfolioRecalculated(event);
            break;
        // ... other event types
    }
}
```

### **2. Web Dashboard (Future)**
```java
// WebSocket client receives real-time updates
@EventListener
public void onMarketDataUpdate(PortfolioEvent event) {
    if (event.getType() == MARKET_DATA_UPDATE) {
        // Update stock price in real-time on web page
        webSocket.send(convertToJson(event));
    }
}
```

### **3. Mobile App (Future)**
```java
// Mobile app receives portfolio updates
@EventListener
public void onPortfolioUpdate(PortfolioEvent event) {
    if (event.getType() == PORTFOLIO_RECALCULATED) {
        // Push notification to user
        pushNotification.send("Portfolio updated: $" + event.getPortfolioSummary().getTotalNav());
    }
}
```

### **4. Risk Management (Future)**
```java
// Risk system monitors for alerts
@EventListener
public void onSystemAlert(PortfolioEvent event) {
    if (event.getSystemAlert().getLevel() == CRITICAL) {
        // Trigger risk management protocols
        riskManager.handleCriticalAlert(event);
    }
}
```

### **5. External Systems (Future)**
```java
// External trading system receives events
@EventListener
public void onPositionUpdate(PortfolioEvent event) {
    if (event.getType() == POSITION_ADDED) {
        // Sync with external trading system
        externalTradingSystem.syncPosition(event.getPositionUpdate());
    }
}
```

## 📊 **Performance Benefits**

### **Protobuf Serialization**
- **Size**: 30-50% smaller than JSON
- **Speed**: 3-10x faster serialization/deserialization
- **Type Safety**: Compile-time validation
- **Versioning**: Schema evolution without breaking changes

### **Event Streaming**
- **Throughput**: Handle thousands of events per second
- **Latency**: Sub-millisecond event delivery
- **Scalability**: Add subscribers without affecting performance
- **Reliability**: Built-in error handling and retry mechanisms

## 🚀 **Production Considerations**

### **Message Brokers**
- **Apache Kafka**: High-throughput, distributed streaming
- **RabbitMQ**: Reliable message queuing
- **Redis Streams**: Lightweight, fast streaming
- **Amazon Kinesis**: Managed streaming service

### **Monitoring & Observability**
- **Event Metrics**: Count, rate, latency per event type
- **Error Tracking**: Failed event deliveries
- **Performance**: Throughput and latency monitoring
- **Alerting**: System health and performance alerts

### **Security**
- **Authentication**: Verify event sources
- **Authorization**: Control who can publish/subscribe
- **Encryption**: Encrypt sensitive event data
- **Audit**: Track all event flows for compliance

## 🎉 **Summary**

Real-time event streaming with Protobuf transforms the portfolio system from a simple console application into a **scalable, real-time platform** that can:

- **Stream live data** to multiple consumers (currently console, future web/mobile)
- **Scale horizontally** by adding more subscribers
- **Integrate easily** with external systems
- **Provide real-time insights** to users
- **Handle high throughput** efficiently
- **Maintain data consistency** across systems

### **Current Implementation Status**
- ✅ **EventBus**: Central event distribution hub
- ✅ **ConsoleEventListener**: Real-time console display with smart logging
- ✅ **Protobuf Events**: Structured, high-performance event messages
- ✅ **Event Publisher**: Type-safe event creation and publishing

This architecture enables the portfolio system to grow from a simple desktop application into a **enterprise-grade real-time trading platform**! 🚀
