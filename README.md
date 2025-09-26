# Real-Time Portfolio Valuation System

A Java-based system for real-time portfolio valuation that supports common stocks and European options (Call/Put) with mock market data simulation.

## Overview

This system provides traders with real-time portfolio valuation capabilities, calculating the Net Asset Value (NAV) of portfolios containing:
- Common stocks
- European Call options on common stocks
- European Put options on common stocks

## Features

- **Portfolio Management**: Load positions from CSV files
- **Security Database**: Embedded database (H2/SQLite) for security definitions
- **Real-time Market Data**: Mock market data provider with geometric Brownian motion simulation
- **Option Pricing**: Black-Scholes model for European options pricing
- **Real-time Updates**: Live portfolio valuation with console output
- **Thread-safe Architecture**: Multi-threaded market data simulation

## Project Structure

```
crypto_java_programming/
├── src/main/java/com/portfolio/
│   ├── PortfolioApplication.java          # Main application class
│   ├── config/
│   │   └── DatabaseConfig.java           # Database configuration
│   ├── model/
│   │   ├── Security.java                 # Security entity
│   │   ├── SecurityType.java            # Security type enum
│   │   ├── Position.java                # Position model
│   │   └── Portfolio.java               # Portfolio model
│   ├── repository/
│   │   └── SecurityRepository.java      # Data access layer
│   └── service/
│       ├── PortfolioManagerService.java # Main orchestration service
│       ├── PositionLoaderService.java   # CSV position loader
│       ├── MarketDataService.java       # Stock price simulation
│       ├── OptionPricingService.java    # Black-Scholes pricing
│       └── PortfolioCalculationService.java # Portfolio calculations
├── src/main/resources/
│   ├── application.properties           # Application configuration
│   ├── schema.sql                      # Database schema
│   ├── data.sql                        # Sample data
│   └── sample-positions.csv            # Sample portfolio positions
├── build.gradle                        # Gradle build configuration
├── setup.bat                          # Windows setup script
├── run.bat                            # Windows run script
└── README.md                          # This file
```

## System Architecture

### Components

1. **Position Loader**: Reads portfolio positions from CSV files
2. **Security Database**: Stores security definitions (stocks, calls, puts)
3. **Market Data Provider**: Simulates stock price movements using geometric Brownian motion
4. **Option Pricing Engine**: Calculates theoretical option prices using Black-Scholes formula
5. **Portfolio Calculator**: Computes real-time market values and NAV
6. **Result Subscriber**: Displays portfolio information in real-time

### Data Flow

```
CSV Positions → Position Loader → Portfolio Calculator
Security Database → Security Definitions → Option Pricing Engine
Market Data Provider → Stock Prices → Portfolio Calculator
Portfolio Calculator → Market Values & NAV → Result Subscriber
```

## Requirements

- **Java**: JDK 1.8 or higher
- **Build Tool**: Gradle
- **Database**: H2 or SQLite (embedded)
- **Dependencies**: Spring, Guava, Protobuf, JUnit, Cucumber

## Prerequisites

- **Java Development Kit (JDK) 8 or higher**
- **Windows PowerShell** (for running batch files)
- **Internet connection** (for downloading dependencies)

## Installation

### Quick Setup (Windows)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd crypto_java_programming
   ```

2. **Run the setup script**
   ```bash
   setup.bat
   ```
   This will:
   - Check Java installation
   - Download all dependencies
   - Build the application
   - Verify everything is working

3. **Run the application**
   ```bash
   run.bat
   ```
   Or alternatively:
   ```bash
   gradlew.bat run
   ```

### Manual Setup

1. **Ensure JDK 1.8+ is installed**
   ```bash
   java -version
   ```

2. **Build the project**
   ```bash
   gradlew.bat build
   ```

3. **Run the application**
   ```bash
   gradlew.bat run
   ```

## Cross-Platform Compatibility

This project is fully cross-platform and works on Windows, macOS, and Linux systems.

### Platform-Specific Commands

| Platform | Build Command | Run Command | Notes |
|----------|---------------|-------------|-------|
| **Windows** | `.\gradlew.bat build` | `.\gradlew.bat run` | Uses batch files |
| **macOS/Linux** | `./gradlew build` | `./gradlew run` | Uses shell scripts |

### macOS/Linux Setup

1. **Make scripts executable** (first time only)
   ```bash
   chmod +x gradlew
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run the application**
   ```bash
   ./gradlew run
   ```

### Cross-Platform Features

- ✅ **Java 8+ compatibility** - Works on all platforms
- ✅ **Gradle wrapper** - Includes both `gradlew` and `gradlew.bat`
- ✅ **H2 database** - Embedded database works everywhere
- ✅ **Spring Framework** - Cross-platform dependency injection
- ✅ **Thread-safe architecture** - Consistent behavior across platforms

### Requirements

- **Java Development Kit (JDK) 1.8 or higher**
- **Gradle** (included via wrapper)
- **No additional dependencies** required

## Usage

### Sample CSV Position File

```csv
symbol,positionSize
AAPL,1000
AAPL-OCT-2020-110-C,-20000
AAPL-OCT-2020-110-P,20000
AAPL-JAN-2026-150-C,5000
AAPL-JAN-2026-150-P,-3000
TELSA,-500
TELSA-NOV-2020-400-C,10000
TELSA-DEC-2020-400-P,-10000
TELSA-FEB-2026-800-C,2000
TELSA-FEB-2026-800-P,-1500

```

### Running the Application

```bash
./gradlew run
```

The system will:
1. Load positions from the CSV file
2. Initialize the security database
3. Start the market data simulation
4. Begin real-time portfolio valuation
5. Display results in the console

### Sample Output

The application displays real-time portfolio updates in the console, showing:
- Total portfolio NAV (Net Asset Value)
- Individual position details with current prices
- Real-time Black-Scholes option pricing
- Market value calculations for each position

![Portfolio Output Screenshot](output_screenshot.png)

*Real-time portfolio valuation showing active options with Black-Scholes pricing*

## Technical Details

### Stock Price Simulation

Stock prices follow a discrete-time geometric Brownian motion:

```
S(t+Δt) = S(t) + ΔS
ΔS = μSΔt + σS√(Δt)ε
```

Where:
- μ = expected return (0-1)
- σ = annualized volatility (0-1)
- ε = random variable from standard normal distribution
- Δt = time interval (0.5-2 seconds)

### Option Pricing

European options are priced using the Black-Scholes formula:

**Call Option:**
```
c = S₀N(d₁) - Ke^(-rt)N(d₂)
```

**Put Option:**
```
p = Ke^(-rt)N(-d₂) - S₀N(-d₁)
```

Where:
- d₁ = [ln(S₀/K) + (r + σ²/2)t] / (σ√t)
- d₂ = d₁ - σ√t
- r = risk-free rate (2% per annum)
- N(x) = cumulative standard normal distribution

### Market Value Calculation

- **Stocks**: `Market Value = Position Size × Stock Price`
- **Options**: `Market Value = Position Size × Option Price`
- **Short Positions**: Multiply by -1
- **Portfolio NAV**: Sum of all position market values

## Database Schema

### Security Table
- `ticker` (VARCHAR): Security identifier
- `type` (VARCHAR): STOCK, CALL, PUT
- `strike` (DECIMAL): Strike price (for options)
- `maturity` (DATE): Expiration date (for options)
- `mu` (DECIMAL): Expected return (for stocks)
- `sigma` (DECIMAL): Volatility (for stocks)

## Configuration

- **Risk-free Rate**: 2% per annum
- **Price Update Interval**: 0.5-2 seconds (random)
- **Starting Prices**: Configurable per stock
- **Database**: H2 (in-memory or file-based)

## Output Format

The system provides real-time console output showing:
- Individual position market values
- Total portfolio NAV
- Current stock prices
- Option theoretical prices
- Timestamp information

## Development Notes

- **Thread Safety**: Market data provider runs in separate thread
- **Mock Data**: No real market data integration required
- **Embedded Database**: No external database dependencies
- **Limited Dependencies**: Only specified third-party libraries allowed

## Testing

Run tests using:
```bash
./gradlew test
```

## License

This project is part of a programming challenge and is for educational purposes.

## Contact

For questions about this implementation, please refer to the original requirements document.