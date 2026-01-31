package ru.practicum.ewm.stats.aggregator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties("aggregator.kafka")
@Data
public class AggregatorKafkaProperties {
    private ProducerConfig producer;
    private ConsumerConfig consumer;

    @Data
    public static class ProducerConfig {
        private String topic;
    }

    @Data
    public static class ConsumerConfig {
        private String topic;
        private Duration pollTimeout = Duration.ofMillis(100);
    }
}