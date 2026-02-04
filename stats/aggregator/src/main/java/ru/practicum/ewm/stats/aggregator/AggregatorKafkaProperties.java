package ru.practicum.ewm.stats.aggregator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties("aggregator.kafka")
@Data
public class AggregatorKafkaProperties {
    private ProducerConfig producer = new ProducerConfig();
    private ConsumerConfig consumer = new ConsumerConfig();

    @Data
    public static class ProducerConfig {
        private String topic = "stats.events-similarity.v1";
        private String compressionType = "snappy";
        private int retries = 3;
    }

    @Data
    public static class ConsumerConfig {
        private String topic = "stats.user-actions.v1";
        private Duration pollTimeout = Duration.ofMillis(100);
        private String groupId = "aggregator-group";
        private int concurrency = 3;
    }
}