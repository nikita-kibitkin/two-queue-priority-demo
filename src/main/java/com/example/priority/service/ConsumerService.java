package com.example.priority.service;

import com.example.priority.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final Histogram q1Histogram = new ConcurrentHistogram(100_000L, 3);
    private final Histogram q2Histogram = new ConcurrentHistogram(100_000L, 3);
    private final ConcurrentLinkedQueue<Message> q1 = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> q2 = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @KafkaListener(topics = {"${spring.kafka.high-topic}"}, concurrency = "2")
    public void handleHigh(Message msg) {
        q1.add(msg);
    }

    @KafkaListener(topics = {"${spring.kafka.normal-topic}"}, concurrency = "2")
    public void handleNormal(Message msg) {
        q2.add(msg);
    }

    public Histogram getHighHistogram() {
        return q1Histogram.copy();
    }
    public Histogram getnNormalHistogram() {
        return q2Histogram.copy();
    }

    @Scheduled(fixedDelay = 30)
    public void queueDispatcher() {
        var q1Poll = q1.poll();
        if (q1Poll != null) {
            executor.execute(() -> emulateWorkAndRecordLatency(q1Poll));
            return;
        }
        var q2Poll = q2.poll();
        if (q2Poll != null) {
            executor.execute(() -> emulateWorkAndRecordLatency(q2Poll));
        }
    }

    @SneakyThrows
    private void emulateWorkAndRecordLatency(Message msg) {
        Thread.sleep(100);
        var latency = System.currentTimeMillis() - msg.startTimeMs();
        if (msg.highPriority()) {
            q1Histogram.recordValue(latency);
        } else {
            q2Histogram.recordValue(latency);
        }
        log.info("Latency recorded: {} ms; {}", latency, msg.highPriority() ? "high" : "normal");
        log.info("Queues length: {}-{}   ", q1.size(), q2.size());
    }
}