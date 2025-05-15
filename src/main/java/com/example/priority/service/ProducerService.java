package com.example.priority.service;

import com.example.priority.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerService {
    @Value(value = "${spring.kafka.high-topic}")
    private String highTopic;
    @Value(value = "${spring.kafka.normal-topic}")
    private String normalTopic;
    @Value(value = "${number-of-messages}")
    private Double NUMBER_OF_MESSAGES; // total amount of messages in demo
    @Value(value = "${high-queue-chance}")
    private Double HIGH_QUEUE_CHANCE;    // 50:50 chances
    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final Random random = new Random();
    private final static double LAMBDA = 9;                  // 9 messages per second

    @Scheduled(fixedDelay = 1000000)
    @SneakyThrows
    public void poissonPublish() {
        log.info("Started PoissonPublish. Number of messages={}", NUMBER_OF_MESSAGES);
        for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            double intervalMs = -Math.log(1.0 - random.nextDouble()) / LAMBDA * 1000;
            Thread.sleep((long) intervalMs);
            var now = System.currentTimeMillis();
            boolean highPriority = random.nextDouble() < HIGH_QUEUE_CHANCE; //
            Message message = new Message(now, highPriority, "payload-" + random.nextDouble());
            kafkaTemplate.send(highPriority ? highTopic : normalTopic, message);
            log.info("Sent: {}, interval: {} ms", message, intervalMs);
        }
        log.info("Finished PoissonPublish");
    }
}
