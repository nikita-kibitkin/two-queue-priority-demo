package com.example.priority.service;

import com.example.priority.metrics.LatencyMetrics;
import com.example.priority.metrics.ThroughputMetrics;
import com.example.priority.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final DoubleQueueService dqs;
    private final ExecutorService executor = Executors.newFixedThreadPool(25);
    // Hysteresis thresholds: allow bulk only below LOW_WM; forbid bulk at/above HIGH_WM.
    private static final int LOW_WM = 20;
    private static final int HIGH_WM = 100;
    private static final int BULK_BUDGET = 5; // Give bulk a few slots per tick to avoid starvation.
    private final Semaphore permits = new Semaphore(4, true);
    private volatile boolean bulkAllowed = false; // Hysteresis state

    @KafkaListener(topics = {"${spring.kafka.high-topic}"}, concurrency = "2")
    public void handleHigh(Message msg) {
        dqs.addToHigh(msg);
        log.info("Received high-message {}", msg);
    }

    @KafkaListener(topics = {"${spring.kafka.bulk-topic}"}, concurrency = "2")
    public void handleBulk(Message msg) {
        dqs.addToBulk(msg);
        log.info("Received bulk-message {}", msg);
    }

    @Scheduled(fixedDelay = 30)
    public void queueDispatcher() {
        // 1) Always drain high first (bounded by available permits).
        while (!dqs.isHighEmpty() && permits.tryAcquire()) {
            Message m = dqs.pollHigh();
            if (m == null) break;
            executor.execute(() -> {
                emulateWorkAndRecordMetrics(m);
                permits.release();
            });
        }

        // 2) Update hysteresis state once per tick.
        if (!bulkAllowed && dqs.getHighLag() <= LOW_WM)
            bulkAllowed = true;   // enable bulk only when high lag is clearly low
        if (bulkAllowed && dqs.getHighLag() >= HIGH_WM)
            bulkAllowed = false;  // disable bulk only when high lag is clearly high

        // 3) If allowed, give a small, capped number of bulk slots.
        if (bulkAllowed) {
            int given = 0;
            while (given < BULK_BUDGET && !dqs.isBulkEmpty()) {
                Message m = dqs.pollBulk();
                log.info("Polled bulk-message {}", m);
                if (m == null) break;
                if (submitWithPermit(executor, permits,
                        () -> emulateWorkAndRecordMetrics(m),
                        () -> dqs.addToBulk(m))) {
                    given++;
                } else break; // pool is saturated; stop early
            }
        }
    }

    public static boolean submitWithPermit(Executor executor, Semaphore permits, Runnable task, Runnable onReject) {
        if (!permits.tryAcquire()) return false;
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    log.error("Exception during executor.execute", t);
                } finally {
                    permits.release();           // ALWAYS release
                }
            });
            return true;
        } catch (RejectedExecutionException rex) {
            permits.release();               // return permit if pool rejected
            if (onReject != null) onReject.run();  // e.g., requeue the message
            log.error("RejectedExecutionException during executor.execute", rex);
            return false;
        }
    }


    @SneakyThrows
    private void emulateWorkAndRecordMetrics(Message msg) {
        Thread.sleep(100);
        var latency = System.currentTimeMillis() - msg.startTimeMs();
        if (msg.highPriority()) {
            LatencyMetrics.getHighHistogram().recordValue(latency);
            ThroughputMetrics.incrementHigh();
        } else {
            LatencyMetrics.getBulkHistogram().recordValue(latency);
            ThroughputMetrics.incrementBulk();
        }
        log.info("Latency recorded: {} ms; {}", latency, msg.highPriority() ? "high" : "bulk");
    }
}