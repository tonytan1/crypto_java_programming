package com.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a security in the portfolio system.
 * Can be a stock, call option, or put option.
 */
@Entity
@Table(name = "security")
public class Security {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticker", nullable = false, unique = true, length = 50)
    private String ticker;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SecurityType type;
    
    @Column(name = "strike", precision = 10, scale = 2)
    private BigDecimal strike;
    
    @Column(name = "maturity")
    private LocalDate maturity;
    
    @Column(name = "mu", precision = 10, scale = 6)
    private BigDecimal mu; // Expected return
    
    @Column(name = "sigma", precision = 10, scale = 6)
    private BigDecimal sigma; // Volatility
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
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
