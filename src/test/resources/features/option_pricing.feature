@option
Feature: Option Pricing with Black-Scholes Model
  As a financial analyst
  I want to calculate option prices using the Black-Scholes formula
  So that I can accurately value options in the portfolio

  Background:
    Given the option pricing service is available
    And the risk-free rate is 5%

  Scenario: Calculate call option price
    Given I have an AAPL call option with:
      | Strike Price | $150.00 |
      | Maturity     | 2024-12-20 |
      | Volatility   | 25% |
      | Drift        | 10% |
    And the underlying AAPL price is $155.00
    When I calculate the option price
    Then the call option price should be positive
    And the price should be approximately $8.50
    And the price should be less than the underlying price

  Scenario: Calculate put option price
    Given I have an AAPL put option with:
      | Strike Price | $150.00 |
      | Maturity     | 2024-12-20 |
      | Volatility   | 25% |
      | Drift        | 10% |
    And the underlying AAPL price is $155.00
    When I calculate the option price
    Then the put option price should be positive
    And the price should be less than the strike price

  Scenario: In-the-money call option
    Given I have an AAPL call option with strike $150.00
    And the underlying price is $200.00
    When I calculate the option price
    Then the option should be in-the-money
    And the price should be significantly higher than out-of-the-money
    And the intrinsic value should be $50.00

  Scenario: Out-of-the-money put option
    Given I have an AAPL put option with strike $150.00
    And the underlying price is $200.00
    When I calculate the option price
    Then the option should be out-of-the-money
    And the price should be relatively low
    And the intrinsic value should be $0.00

  Scenario: High volatility impact
    Given I have an AAPL call option with normal volatility (25%)
    And I have another AAPL call option with high volatility (50%)
    And both have the same strike price and underlying price
    When I calculate both option prices
    Then the high volatility option should have a higher price
    And the difference should be significant

  Scenario: Near expiration option
    Given I have an AAPL call option expiring tomorrow
    And the underlying price is $155.00
    And the strike price is $150.00
    When I calculate the option price
    Then the option price should be close to the intrinsic value
    And the time value should be minimal

  Scenario: Put-call parity relationship
    Given I have a call option and put option with identical parameters:
      | Strike Price | $150.00 |
      | Maturity     | 2024-12-20 |
      | Volatility   | 25% |
      | Drift        | 10% |
    And the underlying price is $155.00
    When I calculate both option prices
    Then the put-call parity should hold approximately:
      | Call Price - Put Price = Underlying Price - Strike Price |
    And the relationship should be within $1.00 tolerance
