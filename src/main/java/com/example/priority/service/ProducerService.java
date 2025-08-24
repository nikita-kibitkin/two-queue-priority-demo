package com.example.priority.service;

import com.example.priority.model.Message;
import com.example.priority.util.PoissonLoadGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerService {
    @Value(value = "${spring.kafka.high-topic}")
    private String highTopic;
    @Value(value = "${spring.kafka.bulk-topic}")
    private String bulkTopic;
    @Value(value = "${poisson.duration-minutes}")
    private int DURATION_MINUTES;
    @Value(value = "${high-priority-chance}")
    private Double HIGH_CHANCE;  //0=one queue only. Real case percentage of live (high priority) payments about 20%=0.2
    @Value(value = "${lambda}")
    private Double LAMBDA;  //messages per second
    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final Random random = new Random();
    private final TaskScheduler scheduler;
    private PoissonLoadGenerator poissonGenerator;

    @PostConstruct
    public void poissonPublish() {
        Runnable task = () -> {
            var message = new Message(System.currentTimeMillis(), isHighPriority(), "payload-" + random.nextDouble());
            kafkaTemplate.send(message.highPriority() ? highTopic : bulkTopic, message);
            log.info("Sent in Kafka: {}", message);
        };

        var poissonGenerator = new PoissonLoadGenerator(scheduler, task, LAMBDA);
        poissonGenerator.start(Duration.ofMinutes(DURATION_MINUTES));
        log.info("PoissonGenerator started");
    }

    @PreDestroy
    void shutdown() {
        if (poissonGenerator != null) poissonGenerator.stop();
    }

    private boolean isHighPriority() {
        return random.nextDouble() < HIGH_CHANCE;
    }
}
