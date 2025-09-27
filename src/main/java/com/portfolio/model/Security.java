package com.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing a security in the portfolio system.
 * Can be a stock, call option, or put option.
 */
public class Security {
    
    private Long id;
    
    private String ticker;
    
    private SecurityType type;
    
    private BigDecimal strike;
    
    private LocalDate maturity;
    
    private BigDecimal mu; // Expected return
    
    private BigDecimal sigma; // Volatility
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Constructors
    public Security() {}
    
    public Security(String ticker, SecurityType type, BigDecimal mu, BigDecimal sigma) {
        this.ticker = ticker;
        this.type = type;
        this.mu = mu;
        this.sigma = sigma;
    }
    
    public Security(String ticker, SecurityType type, BigDecimal strike, LocalDate maturity, 
                   BigDecimal mu, BigDecimal sigma) {
        this.ticker = ticker;
        this.type = type;
        this.strike = strike;
        this.maturity = maturity;
        this.mu = mu;
        this.sigma = sigma;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
    
    public SecurityType getType() {
        return type;
    }
    
    public void setType(SecurityType type) {
        this.type = type;
    }
    
    public BigDecimal getStrike() {
        return strike;
    }
    
    public void setStrike(BigDecimal strike) {
        this.strike = strike;
    }
    
    public LocalDate getMaturity() {
        return maturity;
    }
    
    public void setMaturity(LocalDate maturity) {
        this.maturity = maturity;
    }
    
    public BigDecimal getMu() {
        return mu;
    }
    
    public void setMu(BigDecimal mu) {
        this.mu = mu;
    }
    
    public BigDecimal getSigma() {
        return sigma;
    }
    
    public void setSigma(BigDecimal sigma) {
        this.sigma = sigma;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "Security{" +
                "id=" + id +
                ", ticker='" + ticker + '\'' +
                ", type=" + type +
                ", strike=" + strike +
                ", maturity=" + maturity +
                ", mu=" + mu +
                ", sigma=" + sigma +
                '}';
    }
}
