package com.portfolio.repository;

import com.portfolio.model.Security;
import com.portfolio.model.SecurityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Security entities in the database.
 */
@Repository
public class SecurityRepository {
    
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
     */
    public Optional<Security> findByTicker(String ticker) {
        List<Security> securities = jdbcTemplate.query(SELECT_BY_TICKER, securityRowMapper, ticker);
        return securities.isEmpty() ? Optional.empty() : Optional.of(securities.get(0));
    }
    
    /**
     * Finds all securities of a specific type
     */
    public List<Security> findByType(SecurityType type) {
        return jdbcTemplate.query(SELECT_BY_TYPE, securityRowMapper, type.name());
    }
    
    /**
     * Saves a new security to the database
     */
    public Security save(Security security) {
        if (security.getId() == null) {
            jdbcTemplate.update(INSERT,
                    security.getTicker(),
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
                    security.getTicker(),
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
