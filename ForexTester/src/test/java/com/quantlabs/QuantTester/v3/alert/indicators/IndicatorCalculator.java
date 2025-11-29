package com.quantlabs.QuantTester.v3.alert.indicators;

import org.ta4j.core.BarSeries;
import org.json.JSONObject;

public interface IndicatorCalculator {
    String calculate(BarSeries series, JSONObject params, int shift);
}