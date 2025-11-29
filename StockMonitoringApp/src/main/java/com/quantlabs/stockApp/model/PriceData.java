package com.quantlabs.stockApp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.indicator.management.ZScoreCalculator;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class PriceData {
    // Required fields
    public String ticker;
    public double latestPrice;

    // Optional fields
    public String name;
    public String description;
    public String logoid;
    public String updateMode;
    public String type;
    public List<String> typespecs;
    public Double premarketClose;
    public Integer pricescale;
    public Integer minmov;
    public String fractional;
    public Integer minmove2;
    public String currency;
    public Double postmarketClose;
    public Double premarketChange;
    public Double changeFromOpen;
    public Double percentChange;
    public Double postmarketChange;
    public Double gap;
    public Long premarketVolume;
    public Long currentVolume;
    public Long postmarketVolume;
    public List<Map<String, String>> indexes;
    public String exchange;
	public Long previousVolume;
	public double prevLastDayPrice;
	public double averageVol;
	public String analystRating;
	
	public Double premarketHigh;
	public Double high;
	public Double postmarketHigh;
	public Double premarketLow;
	public Double low;
	public Double postmarketLow;
	
	public Double premarketAvgVolPerMin;
	public Double marketAvgVolPerMin;
	public Double postmarketAvgVolPerMin;
	
	public Double premarketHighestPercentile;
	public Double marketHighestPercentile;
	public Double postmarketHighestPercentile; 
	
	public Double premarketLowestPercentile;
	public Double marketLowestPercentile;
	public Double postmarketLowestPercentile; 
	
	private Double open;
	private Double close;
	
	LinkedHashMap<String, AnalysisResult> results;
	// Z-Score results for multiple strategies
    private Map<String, ZScoreCalculator.ZScoreResult> zScoreResults;
	
	
	public PriceData(){}

    private PriceData(Builder builder) {
        this.ticker = builder.ticker;
        this.latestPrice = builder.latestPrice;
        this.name = builder.name;
        this.description = builder.description;
        this.logoid = builder.logoid;
        this.updateMode = builder.updateMode;
        this.type = builder.type;
        this.typespecs = builder.typespecs;
        this.premarketClose = builder.premarketClose;
        this.pricescale = builder.pricescale;
        this.minmov = builder.minmov;
        this.fractional = builder.fractional;
        this.minmove2 = builder.minmove2;
        this.currency = builder.currency;
        this.postmarketClose = builder.postmarketClose;
        this.premarketChange = builder.premarketChange;
        this.changeFromOpen = builder.changeFromOpen;
        this.percentChange = builder.percentChange;
        this.postmarketChange = builder.postmarketChange;
        this.gap = builder.gap;
        this.premarketVolume = builder.premarketVolume;
        this.currentVolume = builder.currentVolume;
        this.postmarketVolume = builder.postmarketVolume;
        this.indexes = builder.indexes;
        this.exchange = builder.exchange;
        
        this.previousVolume = builder.previousVolume;
        
        this.premarketAvgVolPerMin = builder.premarketAvgVolPerMin;
        this.marketAvgVolPerMin = builder.marketAvgVolPerMin;
        this.postmarketAvgVolPerMin = builder.postmarketAvgVolPerMin;
        
        this.premarketHighestPercentile = builder.premarketHighestPercentile;
        this.marketHighestPercentile = builder.marketHighestPercentile;
        this.postmarketHighestPercentile = builder.postmarketHighestPercentile;

        
        this.premarketLowestPercentile = builder.premarketLowestPercentile;
        this.marketLowestPercentile = builder.marketLowestPercentile;
        this.postmarketLowestPercentile = builder.postmarketLowestPercentile;
        
        this.premarketHigh = builder.premarketHigh;
        this.high = builder.high;
        this.open = builder.open;
        this.close = builder.close;
        this.postmarketHigh = builder.postmarketHigh;
        this.premarketLow = builder.premarketLow;
        this.low = builder.low;
        this.postmarketLow = builder.postmarketLow;
    }

    public PriceData(double currentPrice, long prevDailyVol, long currentDailyVol, double percentChange2, double averageVol,
			double prevLastDayPrice) {
		// TODO Auto-generated constructor stub
    	this.latestPrice = currentPrice;
    	this.previousVolume = prevDailyVol;
    	this.prevLastDayPrice = prevLastDayPrice;
    	this.percentChange = percentChange2;
    	this.averageVol = averageVol;
	}

	public PriceData(double currentPrice, long prevDailyVol, long currentDailyVol, double percentChange2, long averageVol, double prevLastDayPrice2) {
		this.latestPrice = currentPrice;
    	this.previousVolume = prevDailyVol;
    	this.prevLastDayPrice = prevLastDayPrice2;
    	this.percentChange = percentChange2;
    	this.averageVol = averageVol;
    	this.currentVolume = currentDailyVol;    	
	}

	public static class Builder {
        // Required parameters
        private String ticker;
        private double latestPrice;

        // Optional parameters - initialized to default values
        private String name = "";
        private String description = "";
        private String logoid = "";
        private String updateMode = "";
        private String type = "";
        private List<String> typespecs = new ArrayList<>();
        private Double premarketClose = null;
        private Double premarketOpen = null;
        private Double close = null;
        private Double open = null;
        private Double postmarketClose = null;
        private Double premarketHigh = null;
        private Double high = null;
        private Double postmarketHigh = null;
        private Double premarketLow = null;
        private Double low = null;
        private Double postmarketLow = null;
        private Double premarketChange = null;
        private Double percentChange = null;        
        private Double changeFromOpen = null;
        private Double postmarketChange = null;
        private Long premarketVolume = null;
        private Long currentVolume = null;
        private Long postmarketVolume = null;
        private Double averageVol = null;
        private Integer pricescale = null;
        private Integer minmov = null;
        private String fractional = "";
        private Integer minmove2 = null;
        private String currency = "USD";        
        private Double gap = null;        
        private List<Map<String, String>> indexes = new ArrayList<>();
        private String exchange = "";
        
        private Double premarketAvgVolPerMin = 0.0;
        private Double marketAvgVolPerMin = 0.0;
        private Double postmarketAvgVolPerMin = 0.0;
        
        private Double premarketHighestPercentile = 0.0;
        private Double marketHighestPercentile = 0.0;
        private Double postmarketHighestPercentile = 0.0;

		
		private double prevLastDayPrice;
		private Double marketCap;
		
		private double premarketLowestPercentile = 0.0;
		private double postmarketLowestPercentile = 0.0;
		private double marketLowestPercentile = 0.0;
		private Long previousVolume = null;
		

        public Builder(String ticker, double latestPrice) {
            if (ticker == null || ticker.isEmpty()) {
                throw new IllegalArgumentException("Ticker cannot be null or empty");
            }
            if (latestPrice < 0) {
                throw new IllegalArgumentException("Price must be positive");
            }
            this.ticker = ticker;
            this.latestPrice = latestPrice;
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        public Builder description(String description) {
            this.description = Objects.requireNonNull(description);
            return this;
        }

        public Builder logoid(String logoid) {
            this.logoid = Objects.requireNonNull(logoid);
            return this;
        }

        public Builder updateMode(String updateMode) {
            this.updateMode = Objects.requireNonNull(updateMode);
            return this;
        }

        public Builder type(String type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        public Builder typespecs(List<String> typespecs) {
            this.typespecs = Objects.requireNonNull(typespecs);
            return this;
        }

        public Builder premarketClose(Double premarketClose) {
            if (premarketClose != null && premarketClose < 0) {
                throw new IllegalArgumentException("Premarket close cannot be negative");
            }
            this.premarketClose = premarketClose;
            return this;
        }
        
        public Builder premarketOpen(Double premarketOpen) {
            if (premarketOpen != null && premarketClose < 0) {
                throw new IllegalArgumentException("Premarket open cannot be negative");
            }
            this.premarketOpen = premarketOpen;
            return this;
        }
        
        public Builder close(Double close) {
            if (close != null && close < 0) {
                throw new IllegalArgumentException("standard close cannot be negative");
            }
            this.close = close;
            return this;
        }

        public Builder pricescale(Integer pricescale) {
            if (pricescale != null && pricescale < 0) {
                throw new IllegalArgumentException("Pricescale cannot be negative");
            }
            this.pricescale = pricescale;
            return this;
        }

        public Builder minmov(Integer minmov) {
            if (minmov != null && minmov < 0) {
                throw new IllegalArgumentException("Minmov cannot be negative");
            }
            this.minmov = minmov;
            return this;
        }

        public Builder fractional(String fractional) {
            this.fractional = Objects.requireNonNull(fractional);
            return this;
        }

        public Builder minmove2(Integer minmove2) {
            if (minmove2 != null && minmove2 < 0) {
                throw new IllegalArgumentException("Minmove2 cannot be negative");
            }
            this.minmove2 = minmove2;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = Objects.requireNonNull(currency);
            return this;
        }

        public Builder postmarketClose(Double postmarketClose) {
            if (postmarketClose != null && postmarketClose < 0) {
                throw new IllegalArgumentException("Postmarket close cannot be negative");
            }
            this.postmarketClose = postmarketClose;
            return this;
        }
        
        public Builder premarketHigh(Double premarketHigh) {
            if (premarketHigh != null && premarketHigh < 0) {
                throw new IllegalArgumentException("premarket High  cannot be negative");
            }
            this.premarketHigh = premarketHigh;
            return this;
        }
        
        public Builder high(Double high) {
            if (high != null && high < 0) {
                throw new IllegalArgumentException("standard high cannot be negative");
            }
            this.high = high;
            return this;
        }
        
        public Builder open(Double open) {
            if (open != null && open < 0) {
                throw new IllegalArgumentException("standard high cannot be negative");
            }
            this.open = open;
            return this;
        }
        
        public Builder postmarketHigh(Double postmarketHigh) {
            if (postmarketHigh != null && postmarketHigh < 0) {
                throw new IllegalArgumentException("postmarket High cannot be negative");
            }
            this.postmarketHigh = postmarketHigh;
            return this;
        }
        
        public Builder premarketLow(Double premarketLow) {
            if (premarketLow != null && premarketLow < 0) {
                throw new IllegalArgumentException("premarket Low cannot be negative");
            }
            this.premarketLow = premarketLow;
            return this;
        }
        
        public Builder low(Double low) {
            if (low != null && low < 0) {
                throw new IllegalArgumentException("Low cannot be negative");
            }
            this.low = low;
            return this;
        }
        
        public Builder postmarketLow(Double postmarketLow) {
            if (postmarketLow != null && postmarketLow < 0) {
                throw new IllegalArgumentException("postmarketLow cannot be negative");
            }
            this.postmarketLow = postmarketLow;
            return this;
        }
        
        public Builder premarketChange(Double premarketChange) {
            this.premarketChange = premarketChange;
            return this;
        }

        public Builder changeFromOpen(Double changeFromOpen) {
            this.changeFromOpen = changeFromOpen;
            return this;
        }

        public Builder percentChange(Double percentChange) {
            this.percentChange = percentChange;
            return this;
        }

        public Builder postmarketChange(Double postmarketChange) {
            this.postmarketChange = postmarketChange;
            return this;
        }

        public Builder gap(Double gap) {
            this.gap = gap;
            return this;
        }

        public Builder premarketVolume(Long premarketVolume) {
            if (premarketVolume != null && premarketVolume < 0) {
                throw new IllegalArgumentException("Premarket volume cannot be negative");
            }
            this.premarketVolume = premarketVolume;
            return this;
        }

        public Builder currentVolume(Long currentVolume) {
            if (currentVolume != null && currentVolume < 0) {
                throw new IllegalArgumentException("Current volume cannot be negative");
            }
            this.currentVolume = currentVolume;
            return this;
        }

        public Builder postmarketVolume(Long postmarketVolume) {
            if (postmarketVolume != null && postmarketVolume < 0) {
                throw new IllegalArgumentException("Postmarket volume cannot be negative");
            }
            this.postmarketVolume = postmarketVolume;
            return this;
        }
        
        public Builder averageVol(double aveVolume) {
            if (aveVolume < 0) {
                throw new IllegalArgumentException("aveVolume cannot be negative");
            }
            this.averageVol = aveVolume;
            return this;
        }

        public Builder indexes(List<Map<String, String>> indexes) {
            this.indexes = Objects.requireNonNull(indexes);
            return this;
        }

        public Builder exchange(String exchange) {
            this.exchange = Objects.requireNonNull(exchange);
            return this;
        }

        public PriceData build() {
            return new PriceData(this);
        }

		public Builder ticker(String ticker) {
			// TODO Auto-generated method stub
			this.ticker = Objects.requireNonNull(ticker);
			return this;
		}

		public Builder latestPrice(double latestPrice) {
			// TODO Auto-generated method stub
			this.latestPrice = Objects.requireNonNull(latestPrice);
			return this;
		}
		
		public Builder prevLastDayPrice(Double prevLastDayPrice) {
			this.prevLastDayPrice = Objects.requireNonNull(prevLastDayPrice);
			return this;
		}

		public Builder marketCap(Double marketCap) {
			// TODO Auto-generated method stub
			this.marketCap = Objects.requireNonNull(marketCap);
			return this;
		}
		
		public Builder premarketAvgVolPerMin(Double premarketAvgVolPerMin) {
		    if (premarketAvgVolPerMin != null && premarketAvgVolPerMin < 0) {
		        throw new IllegalArgumentException("Premarket average volume per minute cannot be negative");
		    }
		    this.premarketAvgVolPerMin = premarketAvgVolPerMin;
		    return this;
		}

		public Builder marketAvgVolPerMin(Double marketAvgVolPerMin) {
		    if (marketAvgVolPerMin != null && marketAvgVolPerMin < 0) {
		        throw new IllegalArgumentException("Market average volume per minute cannot be negative");
		    }
		    this.marketAvgVolPerMin = marketAvgVolPerMin;
		    return this;
		}

		public Builder postmarketAvgVolPerMin(Double postmarketAvgVolPerMin) {
		    if (postmarketAvgVolPerMin != null && postmarketAvgVolPerMin < 0) {
		        throw new IllegalArgumentException("Postmarket average volume per minute cannot be negative");
		    }
		    this.postmarketAvgVolPerMin = postmarketAvgVolPerMin;
		    return this;
		}
		
		
		public Builder premarketHighestPercentile(Double premarketHighestPercentile) {
		    this.premarketHighestPercentile = premarketHighestPercentile;
		    return this;
		}

		public Builder marketHighestPercentile(Double marketHighestPercentile) {
		    this.marketHighestPercentile = marketHighestPercentile;
		    return this;
		}

		public Builder postmarketHighestPercentile(Double postmarketHighestPercentile) {
		    this.postmarketHighestPercentile = postmarketHighestPercentile;
		    return this;
		}

		public Builder premarketLowestPercentile(double premarketLowestPercentile) {
			this.premarketLowestPercentile = premarketLowestPercentile;
		    return this;
		}

		public Builder postmarketLowestPercentile(double postmarketLowestPercentile) {
			this.postmarketLowestPercentile = postmarketLowestPercentile;
		    return this;
		}

		public Builder marketLowestPercentile(double marketLowestPercentile) {
			this.marketLowestPercentile = marketLowestPercentile;
		    return this;
		}

		public Builder previousVolume(Long previousVolume) {
			if (previousVolume != null && previousVolume < 0) {
	            throw new IllegalArgumentException("Previous volume cannot be negative");
	        }
	        this.previousVolume = previousVolume;
	        return this;
		}

    }

    // Getters
    public String getTicker() { return ticker; }
    public double getLatestPrice() { return latestPrice; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLogoid() { return logoid; }
    public String getUpdateMode() { return updateMode; }
    public String getType() { return type; }
    public List<String> getTypespecs() { return typespecs; }
    public Double getPremarketClose() { return premarketClose; }
    public Integer getPricescale() { return pricescale; }
    public Integer getMinmov() { return minmov; }
    public String getFractional() { return fractional; }
    public Integer getMinmove2() { return minmove2; }
    public String getCurrency() { return currency; }
    public Double getPostmarketClose() { return postmarketClose; }
    public Double getPremarketChange() { return premarketChange; }
    public Double getChangeFromOpen() { return changeFromOpen; }
    public Double getPercentChange() { return percentChange; }
    public Double getPostmarketChange() { return postmarketChange; }
    public Double getGap() { return gap; }
    public Long getPremarketVolume() { return premarketVolume; }
    public Long getCurrentVolume() { return currentVolume; }
    public Long getPostmarketVolume() { return postmarketVolume; }
    public List<Map<String, String>> getIndexes() { return indexes; }
    public String getExchange() { return exchange; }
        
    public Double getHigh() {
		return high;
	}

	public void setHigh(Double high) {
		this.high = high;
	}

	public Double getLow() {
		return low;
	}

	public void setLow(Double low) {
		this.low = low;
	}

	public double getPrevLastDayPrice() {
        return prevLastDayPrice;
    }

    public double getAverageVol() {
        return averageVol;
    }

    public Double getOpen() {
		return open;
	}

	public void setOpen(Double open) {
		this.open = open;
	}

	public Double getClose() {
		return close;
	}

	public void setClose(Double close) {
		this.close = close;
	}
	
	// Getters and setters
    public Map<String, ZScoreCalculator.ZScoreResult> getZScoreResults() { 
        if (zScoreResults == null) {
            zScoreResults = new HashMap<>();
        }
        return zScoreResults; 
    }
    
    public void setZScoreResults(Map<String, ZScoreCalculator.ZScoreResult> zScoreResults) { 
        this.zScoreResults = zScoreResults; 
    }
    
    public void addZScoreResult(String strategyName, ZScoreCalculator.ZScoreResult result) {
        getZScoreResults().put(strategyName, result);
    }
    
    public ZScoreCalculator.ZScoreResult getZScoreResult(String strategyName) {
        return getZScoreResults().get(strategyName);
    }
    
    public double getCombinedZScore() {
        return ZScoreCalculator.getCombinedScore(getZScoreResults());
    }
    
    /**
     * Get the best performing strategy for this symbol
     */
    public ZScoreCalculator.ZScoreResult getBestStrategy() {
        return ZScoreCalculator.getBestStrategy(getZScoreResults());
    }
    
    /**
     * Get all strategies sorted by score (highest first)
     */
    public List<ZScoreCalculator.ZScoreResult> getStrategiesByScore() {
        if (zScoreResults == null || zScoreResults.isEmpty()) {
            return new ArrayList<>();
        }
        return zScoreResults.values().stream()
                .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
                .collect(Collectors.toList());
    }

	public Long getPreviousVolume() {
		// TODO Auto-generated method stub
		return this.previousVolume;
	}

	public String getAnalystRating() {
		// TODO Auto-generated method stub
		return this.analystRating;
	}
	
	public Double getPremarketAvgVolPerMin() { return premarketAvgVolPerMin; }
	public Double getMarketAvgVolPerMin() { return marketAvgVolPerMin; }
	public Double getPostmarketAvgVolPerMin() { return postmarketAvgVolPerMin; }
	
	public Double getPremarketHighestPercentile() { return premarketHighestPercentile; }
	public Double getMarketHighestPercentile() { return marketHighestPercentile; }
	public Double getPostmarketHighestPercentile() { return postmarketHighestPercentile; }
		
	public Double getPremarketLowestPercentile() {
		return premarketLowestPercentile;
	}

	public Double getMarketLowestPercentile() {
		return marketLowestPercentile;
	}

	public Double getPostmarketLowestPercentile() {
		return postmarketLowestPercentile;
	}

	public Map<String, AnalysisResult> getResults() {
		return results;
	}

	public void setResults(LinkedHashMap<String, AnalysisResult> results) {
		this.results = results;
	}
	

	@Override
    public String toString() {
        return String.format(
            "PriceData{ticker='%s', latestPrice=%.2f %s, percentChange=%.2f%%, currentVolume=%,d}",
            ticker, latestPrice, currency, percentChange != null ? percentChange : 0, 
            currentVolume != null ? currentVolume : 0
        );
    }
}