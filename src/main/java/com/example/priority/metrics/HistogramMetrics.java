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
        registry.gauge("queue.latency.p99", Tags.of("queue", "main"), consumerService,
                consumer -> consumer.getHistogram().getValueAtPercentile(99));
        registry.gauge("queue.latency.p95", Tags.of("queue", "main"), consumerService,
                consumer -> consumer.getHistogram().getValueAtPercentile(95));
        registry.gauge("queue.latency.p50", Tags.of("queue", "main"), consumerService,
                consumer -> consumer.getHistogram().getValueAtPercentile(50));
    }
}