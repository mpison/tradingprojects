package com.quantlabs.core.trend;

import org.ta4j.core.BarSeries;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

public abstract class ABaseTrend {

	private TimeFrameEnum timeframe;
	protected int shift;
	private TrendClassEnum trendClassEnum;
	private String symbol;
	protected boolean isUseMomentum;
	
	protected BarSeries series; // ✅ Shared across subclasses
	
	protected TrendDirectionEnum trendResult = TrendDirectionEnum.INVALID_UNINITIALIZED;

	public ABaseTrend(TimeFrameEnum timeframe, int shift, TrendClassEnum trendClass, String symbol, boolean isUseMomentum, BarSeries series) {
        this.timeframe = timeframe;
        this.shift = shift;
        this.trendClassEnum = trendClass;
        this.symbol = symbol;
        this.isUseMomentum = isUseMomentum;
        this.series = series;
    }

	public TimeFrameEnum getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(TimeFrameEnum timeframe) {
		this.timeframe = timeframe;
	}

	public int getShift() {
		return shift;
	}

	public void setShift(int shift) {
		this.shift = shift;
	}

	public TrendClassEnum getTrendClassEnum() {
		return trendClassEnum;
	}

	public void setTrendClassEnum(TrendClassEnum trendClassEnum) {
		this.trendClassEnum = trendClassEnum;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public boolean isUseMomentum() {
		return isUseMomentum;
	}

	public void setUseMomentum(boolean useMomentum) {
		isUseMomentum = useMomentum;
	}
	
	public void enableMomentum(boolean enabled) {
	    this.isUseMomentum = enabled;
	}
	
	public BarSeries getSeries() {
        return series;
    }

    public void setSeries(BarSeries series) {
        this.series = series;
    }

	public abstract void calculate();

	public abstract TrendDirectionEnum identifyTrend(int myShift);
	
	// ✅ Default method using internal `shift`
    public TrendDirectionEnum identifyTrend() {
        return identifyTrend(this.shift);
    }
    
    public TrendDirectionEnum getTrendResult() {
        return trendResult;
    }
}
