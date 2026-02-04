package ru.practicum.ewm.stats.aggregator;

import jakarta.annotation.PostConstruct;
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
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter implements ApplicationRunner {
    private final KafkaConsumer<String, UserActionAvro> kafkaConsumer;
    private final KafkaProducer<String, EventSimilarityAvro> kafkaProducer;
    private final SimilarityCalculator similarityCalculator;
    private final AggregatorKafkaProperties kafkaProperties;
    private volatile boolean running = true;

    @Override
    public void run(ApplicationArguments args) {
        new Thread(this::startConsuming, "kafka-aggregator-thread").start();
    }

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            kafkaConsumer.wakeup();
            log.info("Shutdown hook triggered");
        }));
    }

    private void startConsuming() {
        kafkaConsumer.subscribe(Collections.singletonList(kafkaProperties.getConsumer().getTopic()));
        log.info("Subscribed to topic: {}", kafkaProperties.getConsumer().getTopic());

        try {
            while (running) {
                ConsumerRecords<String, UserActionAvro> records = kafkaConsumer.poll(
                        Duration.ofMillis(kafkaProperties.getConsumer().getPollTimeout().toMillis())
                );

                records.forEach(record -> {
                    UserActionAvro message = record.value();
                    log.debug("Получено сообщение: userId={}, eventId={}, action={}",
                            message.getUserId(), message.getEventId(), message.getActionType());

                    long userId = message.getUserId();
                    long eventId = message.getEventId();
                    String actionType = message.getActionType().toString();
                    double weight = similarityCalculator.getActionWeight(actionType);

                    similarityCalculator.processAction(userId, eventId, weight, message.getTimestamp())
                            .forEach(similarityMessage -> {
                                String key = similarityMessage.getEventA() + "_" + similarityMessage.getEventB();
                                kafkaProducer.send(new ProducerRecord<>(
                                        kafkaProperties.getProducer().getTopic(),
                                        key,
                                        similarityMessage
                                ), (metadata, exception) -> {
                                    if (exception != null) {
                                        log.error("Ошибка отправки: {}", exception.getMessage(), exception);
                                    } else {
                                        log.trace("Отправлено сходство: eventA={}, eventB={}, score={}",
                                                similarityMessage.getEventA(),
                                                similarityMessage.getEventB(),
                                                similarityMessage.getScore());
                                    }
                                });
                            });
                });

                if (!records.isEmpty()) {
                    kafkaConsumer.commitSync();
                    log.debug("Committed offsets for {} messages", records.count());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка в потребителе Kafka", e);
        } finally {
            kafkaConsumer.close();
            log.info("Потребитель Kafka остановлен");
        }
    }
}