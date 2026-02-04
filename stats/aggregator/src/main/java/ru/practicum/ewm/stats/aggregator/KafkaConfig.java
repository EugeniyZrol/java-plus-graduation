package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.AvroDeserializer;
import ru.practicum.ewm.stats.kafka.AvroSerializer;


@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AggregatorKafkaProperties.class)
public class KafkaConfig {
    private final AggregatorKafkaProperties properties;

    @Bean
    public KafkaConsumer<String, UserActionAvro> kafkaConsumer(
            AvroDeserializer<UserActionAvro> deserializer) {
        return new KafkaConsumer<>(
                properties.getConsumer().getProperties(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public KafkaProducer<String, EventSimilarityAvro> kafkaProducer(
            AvroSerializer<EventSimilarityAvro> serializer) {
        return new KafkaProducer<>(
                properties.getProducer().getProperties(),
                new StringSerializer(),
                serializer
        );
    }
}