package com.quantlabs.stockApp.core.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.num.Num;

/**
 * Computes SMA(barCount) of volume on the subseries consisting of the last subseriesLength bars.
 * - Step 1: Take tail = series.getSubSeries(start, end+1) where start = max(0, end-(subseriesLength-1))
 * - Step 2: Compute SMA(barCount) on that tail.
 *
 * For any index outside the tail, or before enough data (index < start+(barCount-1)), returns NaN.
 */
public class VolumeMAIndicator extends CachedIndicator<Num> {

    private final int barCount;          // e.g., 20
    private final int subseriesLength;   // e.g., 1000

    // Small cache so we don't rebuild tail/SMA on every call
    private int cachedStart = -1;
    private int cachedEnd   = -1;
    private BarSeries cachedTail;
    private VolumeIndicator cachedVolume;
    private SMAIndicator cachedSma; // SMA over volume on the cachedTail

    public VolumeMAIndicator(BarSeries series, int barCount, int subseriesLength) {
        super(series);
        this.barCount = Math.max(1, barCount);
        this.subseriesLength = Math.max(1, subseriesLength);
    }

    /** Convenience: defaults to SMA(20) over last 1000 bars. */
    public VolumeMAIndicator(BarSeries series) {
        this(series, 20, 1000);
    }

    /** Convenience: defaults to last 1000 bars. */
    public VolumeMAIndicator(BarSeries series, int barCount) {
        this(series, barCount, 1000);
    }

    public int getBarCount() { return barCount; }
    public int getSubseriesLength() { return subseriesLength; }

    @Override
    protected Num calculate(int index) {
        final BarSeries series = getBarSeries();
        final int end = series.getEndIndex();
        if (end < 0) return numOf(Double.NaN);

        final int start = Math.max(0, end - (subseriesLength - 1));

        // If this index isn't in the last-N tail, it's out of scope.
        if (index < start || index > end) {
            return numOf(Double.NaN);
        }

        // Rebuild tail/SMA only if the end (or start) moved.
        if (start != cachedStart || end != cachedEnd || cachedTail == null) {
            cachedStart = start;
            cachedEnd   = end;
            // NOTE: getSubSeries(start, endExclusive)
            cachedTail  = series.getSubSeries(start, end + 1);
            cachedVolume = new VolumeIndicator(cachedTail);
            cachedSma    = new SMAIndicator(cachedVolume, barCount);
        }

        // Translate "index in full series" -> "index in tail"
        final int tailIndex = index - cachedStart;

        // Not enough bars within the tail yet to form an SMA window.
        if (tailIndex < barCount - 1) {
            return numOf(Double.NaN);
        }

        return cachedSma.getValue(tailIndex);
    }
}
