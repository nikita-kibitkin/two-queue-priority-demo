package com.example.priority.metrics;

import com.example.priority.service.DoubleQueueService;
import io.micrometer.core.instrument.Gauge;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GaugeRegistry {
    private final DoubleQueueService doubleQueueService;
    private final io.micrometer.core.instrument.MeterRegistry registry;

    @PostConstruct
    void init() {
        //throughput
        Gauge.builder("high.throughput", ThroughputMetrics::getCurrentThroughputHigh)
                .register(registry);
        Gauge.builder("bulk.throughput", ThroughputMetrics::getCurrentThroughputBulk)
                .register(registry);
        //latency
        Gauge.builder("high.latency.p99", () -> LatencyMetrics.getHighHistogram().getValueAtPercentile(99))
                .register(registry);
        Gauge.builder("high.latency.p95", () -> LatencyMetrics.getHighHistogram().getValueAtPercentile(95))
                .register(registry);
        Gauge.builder("high.latency.p50", () -> LatencyMetrics.getHighHistogram().getValueAtPercentile(50))
                .register(registry);
        Gauge.builder("bulk.latency.p99", () -> LatencyMetrics.getBulkHistogram().getValueAtPercentile(99))
                .register(registry);
        Gauge.builder("bulk.latency.p95", () -> LatencyMetrics.getBulkHistogram().getValueAtPercentile(95))
                .register(registry);
        Gauge.builder("bulk.latency.p50", () -> LatencyMetrics.getBulkHistogram().getValueAtPercentile(50))
                .register(registry);
        //queue lag
        Gauge.builder("bulk.queue.lag", doubleQueueService::getHighLag)
                .register(registry);
        Gauge.builder("bulk.queue.lag", doubleQueueService::getBulkLag)
                .register(registry);
    }
}