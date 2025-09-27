package com.portfolio.repository;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Security entities in the database.
 */
@Repository
public class SecurityRepository implements ISecurityRepository {
    
    private static final Logger logger = Logger.getLogger(SecurityRepository.class.getName());
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String SELECT_ALL = 
            "SELECT id, ticker, type, strike, maturity, mu, sigma, created_at, updated_at FROM security";
    
    private static final String SELECT_BY_TICKER = 
            SELECT_ALL + " WHERE ticker = ?";
    
    private static final String SELECT_BY_TYPE = 
            SELECT_ALL + " WHERE type = ?";
    
    private static final String INSERT = 
            "INSERT INTO security (ticker, type, strike, maturity, mu, sigma) VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE = 
            "UPDATE security SET ticker = ?, type = ?, strike = ?, maturity = ?, mu = ?, sigma = ? WHERE id = ?";
    
    private static final String DELETE = 
            "DELETE FROM security WHERE id = ?";
    
    private static final String DELETE_ALL = 
            "DELETE FROM security";
    
    private final RowMapper<Security> securityRowMapper = new SecurityRowMapper();
    
    /**
     * Finds all securities in the database
     */
    public List<Security> findAll() {
        return jdbcTemplate.query(SELECT_ALL, securityRowMapper);
    }
    
    /**
     * Finds a security by its ticker symbol
     * JdbcTemplate automatically prevents SQL injection with parameterized queries
     */
    public Optional<Security> findByTicker(String ticker) {
        if (!StringUtils.hasText(ticker)) {
            logger.warning("Empty ticker provided for database query");
            return Optional.empty();
        }
        
        // JdbcTemplate parameterized queries automatically prevent SQL injection
        List<Security> securities = jdbcTemplate.query(SELECT_BY_TICKER, securityRowMapper, ticker.trim().toUpperCase());
        return securities.isEmpty() ? Optional.empty() : Optional.of(securities.get(0));
    }
    
    /**
     * Finds all securities of a specific type
     * JdbcTemplate parameterized queries automatically prevent SQL injection
     */
    public List<Security> findByType(SecurityType type) {
        if (type == null) {
            logger.warning("SecurityType cannot be null");
            return new ArrayList<>();
        }
        
        return jdbcTemplate.query(SELECT_BY_TYPE, securityRowMapper, type.name());
    }
    
    /**
     * Saves a new security to the database
     * JPA validation annotations handle input validation
     */
    public Security save(Security security) {
        if (security == null) {
            logger.warning("Cannot save null security");
            throw new IllegalArgumentException("Security cannot be null");
        }
        
        // Basic validation using Spring's StringUtils
        if (!StringUtils.hasText(security.getTicker())) {
            logger.warning("Cannot save security with empty ticker");
            throw new IllegalArgumentException("Security ticker cannot be empty");
        }
        
        if (security.getType() == null) {
            logger.warning("Cannot save security with null type");
            throw new IllegalArgumentException("Security type cannot be null");
        }
        
        if (security.getId() == null) {
            jdbcTemplate.update(INSERT,
                    security.getTicker().trim().toUpperCase(),
                    security.getType().name(),
                    security.getStrike(),
                    security.getMaturity(),
                    security.getMu(),
                    security.getSigma());
            
            // Get the generated ID
            Optional<Security> saved = findByTicker(security.getTicker());
            return saved.orElse(security);
        } else {
            jdbcTemplate.update(UPDATE,
                    security.getTicker().trim().toUpperCase(),
                    security.getType().name(),
                    security.getStrike(),
                    security.getMaturity(),
                    security.getMu(),
                    security.getSigma(),
                    security.getId());
            return security;
        }
    }
    
    /**
     * Deletes a security from the database
     */
    public void deleteById(Long id) {
        jdbcTemplate.update(DELETE, id);
    }
    
    /**
     * Deletes all securities from the database
     */
    public void deleteAll() {
        jdbcTemplate.update(DELETE_ALL);
    }
    
    /**
     * Row mapper for Security entities
     */
    private static class SecurityRowMapper implements RowMapper<Security> {
        @Override
        public Security mapRow(ResultSet rs, int rowNum) throws SQLException {
            Security security = new Security();
            security.setId(rs.getLong("id"));
            security.setTicker(rs.getString("ticker"));
            security.setType(SecurityType.valueOf(rs.getString("type")));
            security.setStrike(rs.getBigDecimal("strike"));
            security.setMaturity(rs.getDate("maturity") != null ? 
                    rs.getDate("maturity").toLocalDate() : null);
            security.setMu(rs.getBigDecimal("mu"));
            security.setSigma(rs.getBigDecimal("sigma"));
            security.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
            security.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return security;
        }
    }
}
