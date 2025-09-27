package com.portfolio.service;

import com.portfolio.model.Security;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Service for calculating option prices using the Black-Scholes formula.
 */
@Service
public class OptionPricingService {
    
    private static final Logger logger = Logger.getLogger(OptionPricingService.class.getName());
    
    @Value("${portfolio.marketdata.risk-free-rate:0.02}")
    private BigDecimal riskFreeRate;
    
    private static final int SCALE = 10;
    
    /**
     * Calculates the theoretical price of a European option using Black-Scholes formula
     */
    public BigDecimal calculateOptionPrice(Security option, BigDecimal underlyingPrice) {
        try {
            // Validate inputs
            if (!validateOptionInputs(option, underlyingPrice)) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal strike = option.getStrike();
            BigDecimal volatility = option.getSigma();
            LocalDate maturity = option.getMaturity();
            LocalDate today = LocalDate.now();
            
            long daysToMaturity = ChronoUnit.DAYS.between(today, maturity);
            if (daysToMaturity < 0) {
                return BigDecimal.ZERO; // Option has expired
            }
            
            // If maturity is today (daysToMaturity == 0), option value equals intrinsic value
            if (daysToMaturity == 0) {
                return calculateIntrinsicValue(option, underlyingPrice);
            }
            
            BigDecimal timeToMaturity = new BigDecimal(daysToMaturity).divide(new BigDecimal(365), SCALE, RoundingMode.HALF_UP);
            
            BigDecimal d1 = calculateD1(underlyingPrice, strike, riskFreeRate, volatility, timeToMaturity);
            BigDecimal d2 = calculateD2(d1, volatility, timeToMaturity);
            
            // Use Switch Expression for cleaner option type handling
            return switch (option.getType()) {
                case CALL -> calculateCallPrice(underlyingPrice, strike, riskFreeRate, timeToMaturity, d1, d2);
                case PUT -> calculatePutPrice(underlyingPrice, strike, riskFreeRate, timeToMaturity, d1, d2);
                case STOCK -> BigDecimal.ZERO; // This should never happen due to earlier validation
            };
            
        } catch (Exception e) {
            logger.severe("Error calculating option price for " + (option != null ? option.getTicker() : "null") + ": " + e.getMessage());
            return BigDecimal.ZERO; // Return safe default instead of throwing
        }
    }
    
    /**
     * Validates option pricing inputs using Java 17 Switch Expressions
     * @param option the security option to validate
     * @param underlyingPrice the underlying asset price
     * @return true if validation passes, false otherwise
     */
    private boolean validateOptionInputs(Security option, BigDecimal underlyingPrice) {
        // Check for null option
        if (option == null) {
            logger.warning("Option cannot be null");
            return false;
        }
        
        // Check for valid underlying price
        if (underlyingPrice == null || underlyingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warning("Underlying price must be positive, got: " + underlyingPrice);
            return false;
        }
        
        // Use Switch Expression for cleaner type checking
        if (!switch (option.getType()) {
            case STOCK -> false;
            case CALL, PUT -> true;
        }) {
            logger.warning("Cannot calculate option price for stock: " + option.getTicker());
            return false;
        }
        
        // Check for required option parameters
        if (option.getStrike() == null || option.getMaturity() == null) {
            logger.warning("Option must have strike price and maturity date: " + option.getTicker());
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculates d1 for Black-Scholes formula
     */
    private BigDecimal calculateD1(BigDecimal S, BigDecimal K, BigDecimal r, BigDecimal sigma, BigDecimal t) {
        BigDecimal lnSK = BigDecimal.valueOf(Math.log(S.doubleValue() / K.doubleValue()));
        BigDecimal numerator = lnSK.add(
            r.add(sigma.multiply(sigma).divide(new BigDecimal("2"), SCALE, RoundingMode.HALF_UP))
              .multiply(t)
        );
        BigDecimal denominator = sigma.multiply(BigDecimal.valueOf(Math.sqrt(t.doubleValue())));
        
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates d2 for Black-Scholes formula
     */
    private BigDecimal calculateD2(BigDecimal d1, BigDecimal sigma, BigDecimal t) {
        return d1.subtract(sigma.multiply(BigDecimal.valueOf(Math.sqrt(t.doubleValue()))));
    }
    
    /**
     * Calculates European call option price
     */
    private BigDecimal calculateCallPrice(BigDecimal S, BigDecimal K, BigDecimal r, BigDecimal t, BigDecimal d1, BigDecimal d2) {
        BigDecimal N_d1 = cumulativeNormalDistribution(d1);
        BigDecimal N_d2 = cumulativeNormalDistribution(d2);
        BigDecimal discountFactor = BigDecimal.valueOf(Math.exp(-r.doubleValue() * t.doubleValue()));
        
        return S.multiply(N_d1).subtract(K.multiply(discountFactor).multiply(N_d2));
    }
    
    /**
     * Calculates European put option price
     */
    private BigDecimal calculatePutPrice(BigDecimal S, BigDecimal K, BigDecimal r, BigDecimal t, BigDecimal d1, BigDecimal d2) {
        BigDecimal N_neg_d1 = cumulativeNormalDistribution(d1.negate());
        BigDecimal N_neg_d2 = cumulativeNormalDistribution(d2.negate());
        BigDecimal discountFactor = BigDecimal.valueOf(Math.exp(-r.doubleValue() * t.doubleValue()));
        
        return K.multiply(discountFactor).multiply(N_neg_d2).subtract(S.multiply(N_neg_d1));
    }
    
    /**
     * Approximates the cumulative normal distribution function N(x)
     * Using the approximation: N(x) ~= 0.5 * (1 + erf(x/sqrt(2)))
     */
    private BigDecimal cumulativeNormalDistribution(BigDecimal x) {
        double xDouble = x.doubleValue();
        double result = 0.5 * (1 + erf(xDouble / Math.sqrt(2)));
        return new BigDecimal(result).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Approximates the error function erf(x)
     * Using the Abramowitz and Stegun approximation
     */
    private double erf(double x) {
        // Abramowitz and Stegun approximation
        double a1 =  0.254829592;
        double a2 = -0.284496736;
        double a3 =  1.421413741;
        double a4 = -1.453152027;
        double a5 =  1.061405429;
        double p  =  0.3275911;
        
        int sign = x >= 0 ? 1 : -1;
        x = Math.abs(x);
        
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        
        return sign * y;
    }
    
    /**
     * Calculates the intrinsic value of an option at maturity using Switch Expression
     * For calls: max(0, S - K)
     * For puts: max(0, K - S)
     */
    private BigDecimal calculateIntrinsicValue(Security option, BigDecimal underlyingPrice) {
        BigDecimal strike = option.getStrike();
        
        return switch (option.getType()) {
            case CALL -> {
                BigDecimal intrinsicValue = underlyingPrice.subtract(strike);
                yield intrinsicValue.compareTo(BigDecimal.ZERO) > 0 ? intrinsicValue : BigDecimal.ZERO;
            }
            case PUT -> {
                BigDecimal intrinsicValue = strike.subtract(underlyingPrice);
                yield intrinsicValue.compareTo(BigDecimal.ZERO) > 0 ? intrinsicValue : BigDecimal.ZERO;
            }
            case STOCK -> BigDecimal.ZERO; // Stocks don't have intrinsic value in this context
        };
    }
}
