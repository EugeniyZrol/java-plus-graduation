package ru.practicum.ewm.stats.aggregator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
@ConfigurationProperties("aggregator.kafka")
@Data
public class AggregatorKafkaProperties {
    private ProducerConfig producer;
    private ConsumerConfig consumer;
    private Topics topics;

    @Data
    public static class Topics {
        private String userActions;
        private String eventsSimilarity;
    }

    @Data
    public static class ProducerConfig {
        private Map<String, Object> properties;
    }

    @Data
    public static class ConsumerConfig {
        private Duration pollTimeout = Duration.ofMillis(100);
        private Map<String, Object> properties;
    }
}