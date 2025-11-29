package com.quantlabs.stockApp.analysis.model;

public enum ComparisonType {
    HISTORICAL,      // Compare against symbol's own history (current implementation)
    CROSS_SYMBOL,    // Compare against other symbols in current dataset
    ABSOLUTE,        // Use absolute values (e.g., for counts where lower is better)
    HYBRID           // Combine historical and cross-symbol comparison
}