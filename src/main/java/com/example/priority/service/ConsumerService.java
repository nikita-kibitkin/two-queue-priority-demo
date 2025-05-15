package com.example.priority.service;

import com.example.priority.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final Histogram histogram = new ConcurrentHistogram(10000000000L, 3);

    @KafkaListener(topics = {"${spring.kafka.high-topic}"}, concurrency = "4", autoStartup = "true")
    public void handleHigh(Message msg) {
        recordLatency(msg);
    }


    @KafkaListener(topics = {"${spring.kafka.normal-topic}"}, concurrency = "4")
    public void handleNormal(Message msg) {
        recordLatency(msg);
    }

    public Histogram getHistogram() {
        return histogram.copy();
    }

    private void recordLatency(Message msg) {
        var now = System.currentTimeMillis();
        var latency = now - msg.startTimeMs();
        log.info("Latency recorded: {} ms", latency);
        histogram.recordValue(latency);
        log.info("Latency recorded: {} ms", latency);
    }
}