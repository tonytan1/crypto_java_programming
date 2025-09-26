@realtime
Feature: Real-time Portfolio Updates
  As a portfolio manager
  I want to monitor real-time portfolio changes
  So that I can react quickly to market movements

  Background:
    Given the portfolio system is running
    And the market data service is active
    And the event bus is initialized

  Scenario: Price change detection and display
    Given I have a portfolio with AAPL stock at $150.00
    And the previous price was $148.50
    When the market data service updates the price to $155.00
    Then the system should detect a price change
    And the console should display "Changes: 1 UP, 0 DOWN, 0 NEW"
    And the AAPL position should show "[UP]" indicator
    And the portfolio summary should be displayed

  Scenario: Multiple price changes
    Given I have a portfolio with multiple stocks:
      | Symbol | Current Price | Previous Price |
      | AAPL   | $155.00      | $150.00        |
      | TSLA   | $800.00      | $810.00        |
      | MSFT   | $300.00      | $300.00        |
    When the market data service updates all prices
    Then the system should detect changes for AAPL and TSLA
    And the console should display "Changes: 1 UP, 1 DOWN, 0 NEW"
    And only AAPL and TSLA should show change indicators
    And MSFT should show "[SAME]" indicator

  Scenario: New position addition
    Given I have an existing portfolio
    When I add a new position for GOOGL
    Then the system should detect it as a new position
    And the console should display "Changes: 0 UP, 0 DOWN, 1 NEW"
    And the GOOGL position should show "[NEW]" indicator

  Scenario: No changes scenario
    Given I have a portfolio with stable prices
    And all current prices equal previous prices
    When the market data service runs an update cycle
    Then no portfolio summary should be displayed
    And the console should not show any change indicators
    And the system should log "No price changes detected"

  Scenario: Event-driven architecture
    Given the event bus is running
    And I have event listeners registered
    When a market data update occurs
    Then a MarketDataUpdate event should be published
    And a PortfolioRecalculated event should be published
    And all registered listeners should receive the events
    And the events should contain the updated price information

  Scenario: Thread safety during concurrent updates
    Given the portfolio system is running
    And multiple price updates occur simultaneously
    When the system processes all updates
    Then no data corruption should occur
    And all calculations should be consistent
    And the final portfolio state should be valid
    And no exceptions should be thrown

  Scenario: Performance monitoring
    Given the portfolio system is running
    When I monitor the system for 1 minute
    Then the average update time should be under 100ms
    And the memory usage should remain stable
    And no memory leaks should be detected
    And the system should handle at least 100 updates per second
