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
    @Value(value = "${spring.kafka.q1-topic}")
    private String q1Topic;
    @Value(value = "${spring.kafka.q2-topic}")
    private String q2Topic;
    @Value(value = "${number-of-messages}")
    private Double NUMBER_OF_MESSAGES;
    @Value(value = "${q1-chance}")
    private Double Q1_CHANCE;  //0=one queue only. Real case percentage of live (high priority) payments about 20%=0.2
    @Value(value = "${lambda}")
    private Double LAMBDA;  //messages per second
    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final Random random = new Random();

    @Scheduled(initialDelay = 5_000, fixedDelay = 1_000_000)
    @SneakyThrows
    public void poissonPublish() {
        log.info("Started PoissonPublish. Number of messages={}", NUMBER_OF_MESSAGES);
        for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            double intervalMs = -Math.log(1.0 - random.nextDouble()) / LAMBDA * 1000;
            Thread.sleep((long) intervalMs);
            var now = System.currentTimeMillis();
            boolean highPriority = random.nextDouble() < Q1_CHANCE; //
            Message message = new Message(now, highPriority, "payload-" + random.nextDouble());
            kafkaTemplate.send(highPriority ? q1Topic : q2Topic, message).get();
            log.info("Sent: {}, interval: {} ms", message, intervalMs);
        }
        log.info("Finished PoissonPublish");
    }
}
