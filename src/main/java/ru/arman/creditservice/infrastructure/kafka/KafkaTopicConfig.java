package ru.arman.creditservice.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Value("${topics.application-created}")
    private String applicationCreatedTopic;

    @Value("${topics.application-cancelled}")
    private String applicationCancelledTopic;

    @Value("${topics.application-status-changed}")
    private String applicationStatusChangedTopic;

    @Bean
    public NewTopic applicationCreatedTopic() {
        return TopicBuilder.name(applicationCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic applicationCancelledTopic() {
        return TopicBuilder.name(applicationCancelledTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic applicationStatusChangedTopic() {
        return TopicBuilder.name(applicationStatusChangedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
