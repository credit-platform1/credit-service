package ru.arman.creditservice.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topics")
public record KafkaTopics(
        String applicationCreated,
        String applicationCancelled,
        String applicationStatusChanged
) {
}