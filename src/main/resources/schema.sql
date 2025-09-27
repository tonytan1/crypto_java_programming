-- Security definitions table
-- Stores information about stocks and options available in the portfolio system
CREATE TABLE IF NOT EXISTS security (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique identifier for each security',
    ticker VARCHAR(50) NOT NULL UNIQUE COMMENT 'Symbol identifier (e.g., AAPL, AAPL-CALL-150-2025)',
    type VARCHAR(10) NOT NULL CHECK (type IN ('STOCK', 'CALL', 'PUT')) COMMENT 'Security type: STOCK, CALL (call option), or PUT (put option)',
    strike DECIMAL(10,2) COMMENT 'Strike price for options (NULL for stocks)',
    maturity DATE COMMENT 'Expiration date for options (NULL for stocks)',
    mu DECIMAL(10,6) DEFAULT 0.05 COMMENT 'Expected return rate (drift) for geometric Brownian motion simulation',
    sigma DECIMAL(10,6) DEFAULT 0.20 COMMENT 'Volatility (standard deviation) for price simulation',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last modification timestamp'
);

-- Indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_security_ticker ON security(ticker) COMMENT 'Index for quick ticker symbol lookups';
CREATE INDEX IF NOT EXISTS idx_security_type ON security(type) COMMENT 'Index for filtering by security type (STOCK/CALL/PUT)';
