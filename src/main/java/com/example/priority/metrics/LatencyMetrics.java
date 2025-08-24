package com.example.priority.metrics;

import lombok.Getter;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.springframework.stereotype.Component;

@Component
public class LatencyMetrics {
    @Getter
    private final static Histogram highLatencyHist = new ConcurrentHistogram(100_000L, 3);
    @Getter
    private final static Histogram bulkLatencyHist = new ConcurrentHistogram(100_000L, 3);

    public static Histogram getHighHistogram() {
        return highLatencyHist.copy();
    }

    public static Histogram getBulkHistogram() {
        return bulkLatencyHist.copy();
    }
}
