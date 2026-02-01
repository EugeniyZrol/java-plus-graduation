package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter implements ApplicationRunner {

    private final KafkaConsumer<String, UserActionAvro> kafkaConsumer;
    private final KafkaProducer<String, EventSimilarityAvro> kafkaProducer;
    private final SimilarityCalculator similarityCalculator;
    private final AggregatorKafkaProperties kafkaProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this::startKafkaConsumer, "kafka-aggregator-thread").start();
    }

    private void startKafkaConsumer() {
        kafkaConsumer.subscribe(Collections.singletonList(kafkaProperties.getConsumer().getTopic()));
        log.info("Подписан на топик: {}", kafkaProperties.getConsumer().getTopic());

        while (true) {
            ConsumerRecords<String, UserActionAvro> records = kafkaConsumer.poll(
                    Duration.ofMillis(kafkaProperties.getConsumer().getPollTimeout().toMillis())
            );

            records.forEach(record -> {
                UserActionAvro message = record.value();
                log.info("Получено сообщение: userId={}, eventId={}, action={}",
                        message.getUserId(), message.getEventId(), message.getActionType());

                long userId = message.getUserId();
                long eventId = message.getEventId();
                String actionType = message.getActionType().toString();
                double weight = similarityCalculator.getActionWeight(actionType);

                similarityCalculator.processAction(userId, eventId, weight);
            });

            if (!records.isEmpty()) {
                sendUpdatedSimilarities();
                kafkaConsumer.commitSync();
                log.debug("Зафиксированы offsets для {} сообщений", records.count());
            }
        }
    }

    private void sendUpdatedSimilarities() {
        Set<Long> updatedEvents = similarityCalculator.getUpdatedEvents();
        Set<Long> allEventIds = similarityCalculator.getUserWeights().keySet();

        for (Long eventA : updatedEvents) {
            for (Long eventB : allEventIds) {
                if (eventA.equals(eventB)) continue;

                double similarity = similarityCalculator.getCosineSimilarity(eventA, eventB);

                if (similarityCalculator.shouldSendSimilarity(eventA, eventB, similarity)) {
                    long first = Math.min(eventA, eventB);
                    long second = Math.max(eventA, eventB);

                    EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                            .setEventA(first)
                            .setEventB(second)
                            .setScore(similarity)
                            .setTimestamp(Instant.now())
                            .build();

                    String key = first + "_" + second;
                    kafkaProducer.send(new ProducerRecord<>(
                            kafkaProperties.getProducer().getTopic(),
                            key,
                            similarityAvro
                    ));
                    log.debug("Отправлено сходство: eventA={}, eventB={}, score={}",
                            first, second, similarity);
                }
            }
        }

        similarityCalculator.clearUpdatedEvents();
    }
}