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
    private final Histogram highHistogram = new ConcurrentHistogram(100_000L, 3);
    private final Histogram normalHistogram = new ConcurrentHistogram(100_000L, 3);
    private final ConcurrentLinkedQueue<Message> highPriorityQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> normalPriorityQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @KafkaListener(topics = {"${spring.kafka.high-topic}"}, concurrency = "2")
    public void handleHigh(Message msg) {
        highPriorityQueue.add(msg);
    }

    @KafkaListener(topics = {"${spring.kafka.normal-topic}"}, concurrency = "2")
    public void handleNormal(Message msg) {
        normalPriorityQueue.add(msg);
    }

    public Histogram getHighHistogram() {
        return highHistogram.copy();
    }
    public Histogram getnNormalHistogram() {
        return normalHistogram.copy();
    }

    @Scheduled(fixedDelay = 30)
    public void processMessages() {
        var highPoll = highPriorityQueue.poll();
        if (highPoll != null) {
            executor.execute(() -> emulateWorkAndRecordLatency(highPoll));
            return;
        }
        var normalPoll = normalPriorityQueue.poll();
        if (normalPoll != null) {
            executor.execute(() -> emulateWorkAndRecordLatency(normalPoll));
        }
    }

    @SneakyThrows
    private void emulateWorkAndRecordLatency(Message msg) {
        Thread.sleep(100);
        var latency = System.currentTimeMillis() - msg.startTimeMs();
        if (msg.highPriority()) {
            highHistogram.recordValue(latency);
        } else {
            normalHistogram.recordValue(latency);
        }
        log.info("Latency recorded: {} ms; {}", latency, msg.highPriority() ? "high" : "normal");
        log.info("Queues length: {}-{}   ", highPriorityQueue.size(), normalPriorityQueue.size());
    }
}