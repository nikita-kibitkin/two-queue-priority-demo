package com.example.priority.metrics;

import com.example.priority.service.ConsumerService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistogramMetrics {
    private final ConsumerService consumerService;
    private final MeterRegistry registry;

    @PostConstruct
    void init() {
        registry.gauge("high.queue.latency.p99",  consumerService,
                consumer -> consumer.getHighHistogram().getValueAtPercentile(99));
        registry.gauge("high.queue.latency.p95",  consumerService,
                consumer -> consumer.getHighHistogram().getValueAtPercentile(95));
        registry.gauge("high.queue.latency.p50",  consumerService,
                consumer -> consumer.getHighHistogram().getValueAtPercentile(50));
        
        registry.gauge("normal.queue.latency.p99",  consumerService,
                consumer -> consumer.getnNormalHistogram().getValueAtPercentile(99));
        registry.gauge("normal.queue.latency.p95",  consumerService,
                consumer -> consumer.getnNormalHistogram().getValueAtPercentile(95));
        registry.gauge("normal.queue.latency.p50",  consumerService,
                consumer -> consumer.getnNormalHistogram().getValueAtPercentile(50));
    }
}