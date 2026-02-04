package ru.practicum.ewm.stats.aggregator;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aggregator.kafka")
public class AggregatorKafkaProperties {

    private ConsumerProperties consumer = new ConsumerProperties();
    private ProducerProperties producer = new ProducerProperties();
    private Map<String, String> topics = new HashMap<>();

    @Getter
    @Setter
    public static class ConsumerProperties {
        private String bootstrapServers;
        private String keyDeserializer;
        private String groupId;
        private String autoOffsetReset;
        private Boolean enableAutoCommit;
        private Integer maxPollRecords;
        private Integer pollTimeoutMs;
    }

    @Getter
    @Setter
    public static class ProducerProperties {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
        private String acks;
        private Integer retries;
        private String compressionType;
        private Integer batchSize;
        private Integer lingerMs;
    }

    public String getUserActionsTopic() {
        return topics.get("user-actions");
    }

    public String getEventsSimilarityTopic() {
        return topics.get("events-similarity");
    }
}