package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.AvroDeserializer;
import ru.practicum.ewm.stats.kafka.AvroSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
    private final AggregatorKafkaProperties properties;

    @Bean
    public AvroDeserializer<UserActionAvro> userActionAvroDeserializer() {
        return new AvroDeserializer<>(UserActionAvro.class);
    }

    @Bean
    public AvroSerializer<EventSimilarityAvro> eventSimilarityAvroSerializer() {
        return new AvroSerializer<>();
    }

    @Bean
    public KafkaConsumer<String, UserActionAvro> kafkaConsumer(
            AvroDeserializer<UserActionAvro> userActionAvroDeserializer) {
        Map<String, Object> props = new HashMap<>();

        AggregatorKafkaProperties.ConsumerProperties consumerProps = properties.getConsumer();

        if (consumerProps.getBootstrapServers() != null) {
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerProps.getBootstrapServers());
        }

        if (consumerProps.getGroupId() != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerProps.getGroupId());
        }

        if (consumerProps.getAutoOffsetReset() != null) {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerProps.getAutoOffsetReset());
        }

        if (consumerProps.getEnableAutoCommit() != null) {
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerProps.getEnableAutoCommit());
        }

        if (consumerProps.getMaxPollRecords() != null) {
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerProps.getMaxPollRecords());
        }

        StringDeserializer keyDeserializer = new StringDeserializer();

        return new KafkaConsumer<>(props, keyDeserializer, userActionAvroDeserializer);
    }

    @Bean
    public KafkaProducer<String, EventSimilarityAvro> kafkaProducer(
            AvroSerializer<EventSimilarityAvro> eventSimilarityAvroSerializer) {
        Map<String, Object> props = new HashMap<>();

        AggregatorKafkaProperties.ProducerProperties producerProps = properties.getProducer();

        if (producerProps.getBootstrapServers() != null) {
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerProps.getBootstrapServers());
        }

        if (producerProps.getAcks() != null) {
            props.put(ProducerConfig.ACKS_CONFIG, producerProps.getAcks());
        }

        if (producerProps.getRetries() != null) {
            props.put(ProducerConfig.RETRIES_CONFIG, producerProps.getRetries());
        }

        if (producerProps.getCompressionType() != null) {
            props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producerProps.getCompressionType());
        }

        if (producerProps.getBatchSize() != null) {
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, producerProps.getBatchSize());
        }

        if (producerProps.getLingerMs() != null) {
            props.put(ProducerConfig.LINGER_MS_CONFIG, producerProps.getLingerMs());
        }

        StringSerializer keySerializer = new StringSerializer();

        return new KafkaProducer<>(props, keySerializer, eventSimilarityAvroSerializer);
    }
}