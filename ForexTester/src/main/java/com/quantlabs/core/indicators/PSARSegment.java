package com.quantlabs.core.indicators;


import java.util.ArrayList;
import java.util.List;

import com.quantlabs.core.enums.TrendDirectionEnum;

public class PSARSegment {

    private final TrendDirectionEnum direction;
    private final List<Integer> indices = new ArrayList<>();

    public PSARSegment(TrendDirectionEnum direction) {
        this.direction = direction;
    }

    public void addIndex(int index) {
        indices.add(index);
    }

    public TrendDirectionEnum getDirection() {
        return direction;
    }

    public int getLastIndex() {
        return indices.isEmpty() ? -1 : indices.get(indices.size() - 1);
    }

    public int getFirstIndex() {
        return indices.isEmpty() ? -1 : indices.get(0);
    }

    public int getLength() {
        return indices.size();
    }

    public List<Integer> getIndices() {
        return indices;
    }
} 
