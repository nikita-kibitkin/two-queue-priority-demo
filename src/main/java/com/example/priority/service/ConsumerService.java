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

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final Histogram histogram = new ConcurrentHistogram(100_000L, 3);
    private final ConcurrentLinkedQueue<Message> highPriorityQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> normalPriorityQueue = new ConcurrentLinkedQueue<>();

    @KafkaListener(topics = {"${spring.kafka.high-topic}"}, concurrency = "4", autoStartup = "true")
    public void handleHigh(Message msg) {
        highPriorityQueue.add(msg);
    }

    @KafkaListener(topics = {"${spring.kafka.normal-topic}"}, concurrency = "4")
    public void handleNormal(Message msg) {
        normalPriorityQueue.add(msg);
    }

    public Histogram getHistogram() {
        return histogram.copy();
    }

    @Scheduled(fixedDelay = 100)
    public void processMessages() {
        if (highPriorityQueue.isEmpty() && normalPriorityQueue.isEmpty()) {
            return;
        }
        if (!highPriorityQueue.isEmpty()) {
            emulateWorkAndRecordLatency(highPriorityQueue.poll());
        } else {
            emulateWorkAndRecordLatency(normalPriorityQueue.poll());
        }
    }

    @SneakyThrows
    private void emulateWorkAndRecordLatency(Message msg) {
        Thread.sleep(50);
        var latency = System.currentTimeMillis() - msg.startTimeMs();
        histogram.recordValue(latency);
        log.info("Latency recorded: {} ms", latency);
        log.info("Queues length: {}-{}   ", highPriorityQueue.size(), normalPriorityQueue.size());
    }
}