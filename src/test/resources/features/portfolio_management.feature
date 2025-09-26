@portfolio
Feature: Portfolio Management
  As a portfolio manager
  I want to manage investment portfolios
  So that I can track real-time valuations and make informed decisions

  Background:
    Given the portfolio system is initialized
    And the market data service is running

  Scenario: Initialize portfolio from CSV data
    Given I have a CSV file with the following positions:
      | Symbol | Size |
      | AAPL   | 100  |
      | TSLA   | 50   |
      | MSFT   | 75   |
    When I load the portfolio
    Then I should have 3 positions
    And the portfolio should be initialized successfully
    And all positions should have security definitions
    And the portfolio NAV should be calculated

  Scenario: Real-time portfolio updates
    Given the portfolio is initialized with AAPL stock
    And the current AAPL price is $150.00
    When the market data service updates AAPL price to $155.00
    Then the portfolio should recalculate automatically
    And the console should display the updated summary
    And the AAPL position should show a price increase
    And the total NAV should reflect the new price

  Scenario: Portfolio with mixed asset types
    Given I have a portfolio with:
      | Symbol | Type | Size | Strike | Maturity |
      | AAPL   | STOCK| 100  | N/A    | N/A      |
      | AAPL_CALL_150_2024 | CALL | 10 | 150.00 | 2024-12-20 |
      | AAPL_PUT_150_2024  | PUT  | 5  | 150.00 | 2024-12-20 |
    When I calculate the portfolio values
    Then the stock position should have a market value
    And the call option should have a positive value
    And the put option should have a positive value
    And the total portfolio NAV should be the sum of all positions

  Scenario: Handle missing security definitions
    Given I have a portfolio with unknown securities:
      | Symbol | Size |
      | UNKNOWN| 100  |
      | AAPL   | 50   |
    When I load the portfolio
    Then the system should handle missing securities gracefully
    And only valid positions should be processed
    And a warning should be logged for missing securities

  Scenario: Portfolio summary display
    Given the portfolio is initialized with multiple positions
    When I request a portfolio summary
    Then the summary should include:
      | Field | Description |
      | Total NAV | Current portfolio value |
      | Position Count | Number of positions |
      | Last Updated | Timestamp of last calculation |
      | Individual Positions | Symbol, size, market value |
