package com.quantlabs.QuantTester.v3.alert.indicators;

import java.util.HashMap;
import java.util.Map;

public class IndicatorCalculatorFactory {
    private final Map<String, IndicatorCalculator> calculators;

    public IndicatorCalculatorFactory() {
        this.calculators = new HashMap<>();
        registerCalculators();
    }

    private void registerCalculators() {
        calculators.put("MACD", new MACDCalculator());
        calculators.put("RSI", new RSICalculator());
        calculators.put("PSAR", new ParabolicSarCalculator());
        calculators.put("Stochastic", new StochasticCalculator());
        calculators.put("Heikin-Ashi", new HeikinAshiCalculator());
    }

    public IndicatorCalculator getCalculator(String indicatorType) {
        return calculators.get(indicatorType);
    }
}