package com.quantlabs.stockApp.analysis.util;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

import java.lang.reflect.Method;

public class MetricValueExtractor {
    
    public static Object extractMetricValue(PriceData symbol, String metricFullName) throws Exception {
        String[] parts = metricFullName.split("_", 2);
        String timeframe = parts.length > 1 ? parts[0] : null;
        String metricName = parts.length > 1 ? parts[1] : parts[0];
        
        if (timeframe == null) {
            // PriceData metric
            Method method = PriceData.class.getMethod("get" + Character.toUpperCase(metricName.charAt(0)) + metricName.substring(1));
            return method.invoke(symbol);
        } else {
            // AnalysisResult metric with timeframe
            Method getResultsMethod = PriceData.class.getMethod("getResults");
            @SuppressWarnings("unchecked")
            java.util.Map<String, AnalysisResult> results = (java.util.Map<String, AnalysisResult>) getResultsMethod.invoke(symbol);
            
            if (results == null) return null;
            
            AnalysisResult result = results.get(timeframe);
            if (result == null) return null;
            
            Method analysisMethod = AnalysisResult.class.getMethod("get" + Character.toUpperCase(metricName.charAt(0)) + metricName.substring(1));
            return analysisMethod.invoke(result);
        }
    }
}