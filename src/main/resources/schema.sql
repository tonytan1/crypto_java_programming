-- Security definitions table
CREATE TABLE IF NOT EXISTS security (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(10) NOT NULL CHECK (type IN ('STOCK', 'CALL', 'PUT')),
    strike DECIMAL(10,2),
    maturity DATE,
    mu DECIMAL(10,6) DEFAULT 0.05,
    sigma DECIMAL(10,6) DEFAULT 0.20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Index for faster lookups
CREATE INDEX IF NOT EXISTS idx_security_ticker ON security(ticker);
CREATE INDEX IF NOT EXISTS idx_security_type ON security(type);

-- Sample data will be inserted by the application on startup
