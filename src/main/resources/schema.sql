-- ============================================================================
-- PORTFOLIO DATABASE SCHEMA
-- ============================================================================
-- Security definitions table
-- Stores information about stocks and options available in the portfolio system
-- 
-- Table Structure:
-- - id: Unique identifier for each security (auto-increment primary key)
-- - ticker: Symbol identifier (e.g., AAPL, AAPL-CALL-150-2025)
-- - type: Security type - STOCK, CALL (call option), or PUT (put option)
-- - strike: Strike price for options (NULL for stocks)
-- - maturity: Expiration date for options (NULL for stocks)
-- - mu: Expected return rate (drift) for geometric Brownian motion simulation
-- - sigma: Volatility (standard deviation) for price simulation
-- - created_at: Record creation timestamp
-- - updated_at: Last modification timestamp
-- 
-- Notes:
-- - mu (drift) and sigma (volatility) are used in geometric Brownian motion
-- - Default values: mu=0.05 (5% annual return), sigma=0.20 (20% annual volatility)
-- - Option tickers follow pattern: UNDERLYING-TYPE-STRIKE-EXPIRY (e.g., AAPL-CALL-150-2025)
-- - Strike and maturity are NULL for stocks, required for options
-- ============================================================================

CREATE TABLE IF NOT EXISTS security (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                    -- Unique identifier for each security
    ticker VARCHAR(50) NOT NULL UNIQUE,                      -- Symbol identifier (e.g., AAPL, AAPL-CALL-150-2025)
    type VARCHAR(10) NOT NULL CHECK (type IN ('STOCK', 'CALL', 'PUT')), -- Security type: STOCK, CALL, or PUT
    strike DECIMAL(10,2),                                    -- Strike price for options (NULL for stocks)
    maturity DATE,                                           -- Expiration date for options (NULL for stocks)
    mu DECIMAL(10,6) DEFAULT 0.05,                          -- Expected return rate (drift) for price simulation
    sigma DECIMAL(10,6) DEFAULT 0.20,                       -- Volatility (standard deviation) for price simulation
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,         -- Record creation timestamp
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- Last modification timestamp
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================
-- Indexes for faster lookups and query optimization
CREATE INDEX IF NOT EXISTS idx_security_ticker ON security(ticker);  -- Index for quick ticker symbol lookups
CREATE INDEX IF NOT EXISTS idx_security_type ON security(type);      -- Index for filtering by security type (STOCK/CALL/PUT);

-- ============================================================================
