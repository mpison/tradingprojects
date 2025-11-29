package com.quantlabs.QuantTester.v4.alert;

import org.json.JSONObject;
import org.json.JSONException;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.Num;
import org.ta4j.core.Bar;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public class IndicatorCalculator {
    private Duration getBarDuration(String timeframe) {
        switch (timeframe.toLowerCase()) {
            case "1m": return Duration.ofMinutes(1);
            case "5m": return Duration.ofMinutes(5);
            case "15m": return Duration.ofMinutes(15);
            case "30m": return Duration.ofMinutes(30);
            case "1h": return Duration.ofHours(1);
            case "4h": return Duration.ofHours(4);
            case "1d": return Duration.ofDays(1);
            default: return Duration.ofHours(1);
        }
    }

    public String calculateIndicator(String indType, List<DataFetcher.OHLCDataItem> data, JSONObject params, int shift, String timeframe) {
        if (data == null || data.size() < 2) {
            return null;
        }

        BarSeries series = new BaseBarSeries("series");
        Duration barDuration = getBarDuration(timeframe);
        for (DataFetcher.OHLCDataItem item : data) {
            if (item == null || Double.isNaN(item.open) || Double.isNaN(item.high) || 
                Double.isNaN(item.low) || Double.isNaN(item.close) || Double.isNaN(item.volume)) {
                continue;
            }
            try {
                series.addBar(new BaseBar(
                        barDuration,
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.timestamp), ZoneOffset.UTC),
                        series.numOf(item.open),
                        series.numOf(item.high),
                        series.numOf(item.low),
                        series.numOf(item.close),
                        series.numOf(item.volume),
                        series.numOf(10000) // Example amount
                ));
            } catch (Exception e) {
                System.err.println("Error adding bar: " + e.getMessage());
            }
        }

        if (series.getBarCount() < 2) {
            return null; // Not enough bars for calculations
        }

        try {
            switch (indType) {
                case "MACD":
                    int shortTimeFrame = params.getInt("shortTimeFrame");
                    int longTimeFrame = params.getInt("longTimeFrame");
                    int signalTimeFrame = params.getInt("signalTimeFrame");
                    MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), shortTimeFrame, longTimeFrame);
                    EMAIndicator signal = new EMAIndicator(macd, signalTimeFrame);
                    int index = series.getEndIndex() - shift;
                    if (index < 1) return null;
                    double macdValue = macd.getValue(index).doubleValue();
                    double signalValue = signal.getValue(index).doubleValue();
                    double prevMacdValue = macd.getValue(index - 1).doubleValue();
                    double prevSignalValue = signal.getValue(index - 1).doubleValue();
                    if (macdValue > signalValue && prevMacdValue <= prevSignalValue) {
                        return "MACD Bullish Crossover";
                    } else if (macdValue < signalValue && prevMacdValue >= prevSignalValue) {
                        return "MACD Bearish Crossover";
                    }
                    return null;

                case "PSAR":
                	double accelerationFactor = params.getDouble("accelerationFactor");
                    double maxAcceleration = params.getDouble("maxAcceleration");
                    ParabolicSarIndicator psar = new ParabolicSarIndicator(series, series.numOf(accelerationFactor), series.numOf(maxAcceleration));
                    index = series.getEndIndex() - shift;
                    if (index < 1) return null;
                    double psarValue = psar.getValue(index).doubleValue();
                    double prevPsarValue = psar.getValue(index - 1).doubleValue();
                    double close = series.getBar(index).getClosePrice().doubleValue();
                    double prevClose = series.getBar(index - 1).getClosePrice().doubleValue();
                    if (close > psarValue && prevClose <= prevPsarValue) {
                        return "PSAR Bullish Reversal";
                    } else if (close < psarValue && prevClose >= prevPsarValue) {
                        return "PSAR Bearish Reversal";
                    }
                    return null;

                case "RSI":
                    int timeFrame = params.getInt("timeFrame");
                    RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), timeFrame);
                    index = series.getEndIndex() - shift;
                    if (index < 0) return null;
                    double rsiValue = rsi.getValue(index).doubleValue();
                    if (rsiValue > 70) {
                        return "RSI Overbought";
                    } else if (rsiValue < 30) {
                        return "RSI Oversold";
                    }
                    return null;

                case "Heikin-Ashi":
                    index = series.getEndIndex() - shift;
                    if (index < 1) return null;
                    Bar currentBar = series.getBar(index);
                    Bar prevBar = series.getBar(index - 1);
                    if (currentBar == null || prevBar == null) {
                        return null;
                    }
                    double haClose = (currentBar.getOpenPrice().doubleValue() + currentBar.getHighPrice().doubleValue() +
                            currentBar.getLowPrice().doubleValue() + currentBar.getClosePrice().doubleValue()) / 4;
                    double haOpen = (prevBar.getOpenPrice().doubleValue() + prevBar.getClosePrice().doubleValue()) / 2;
                    if (haClose > haOpen && currentBar.getClosePrice().doubleValue() > haOpen) {
                        return "Heikin-Ashi Bullish";
                    } else if (haClose < haOpen && currentBar.getClosePrice().doubleValue() < haOpen) {
                        return "Heikin-Ashi Bearish";
                    }
                    return null;

                default:
                    return null;
            }
        } catch (JSONException e) {
            System.err.println("JSON parsing error: " + e.getMessage());
            return null;
        } catch (NullPointerException e) {
            System.err.println("Null pointer in indicator calculation: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error in indicator calculation: " + e.getMessage());
            return null;
        }
    }
}