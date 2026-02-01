package ru.practicum.ewm.stats.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.UserInteractionEntity;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.dto.ActionWeights;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaAnalyzerConsumer {

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;

    @KafkaListener(topics = "stats.events-similarity.v1", containerFactory = "similarityContainerFactory")
    @Transactional
    public void consumeSimilarity(EventSimilarityAvro message) {
        try {
            log.info("Получено сходство: eventA={}, eventB={}, score={}",
                    message.getEventA(), message.getEventB(), message.getScore());

            Long eventA = Math.min(message.getEventA(), message.getEventB());
            Long eventB = Math.max(message.getEventA(), message.getEventB());

            similarityRepository.deleteByEventAAndEventB(eventA, eventB);

            EventSimilarityEntity entity = new EventSimilarityEntity();
            entity.setEventA(eventA);
            entity.setEventB(eventB);
            entity.setScore(message.getScore());
            entity.setUpdatedAt(message.getTimestamp());

            similarityRepository.save(entity);

        } catch (Exception e) {
            log.error("Ошибка обработки сообщения о сходстве: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "stats.user-actions.v1", containerFactory = "userActionContainerFactory")
    @Transactional
    public void consumeUserAction(UserActionAvro message) {
        try {
            log.info("Получено действие: userId={}, eventId={}, action={}",
                    message.getUserId(), message.getEventId(), message.getActionType());

            double weight = getActionWeight(message.getActionType().toString());

            UserInteractionEntity entity = interactionRepository
                    .findByUserIdAndEventId(message.getUserId(), message.getEventId())
                    .orElse(new UserInteractionEntity());

            if (entity.getMaxWeight() == null || weight > entity.getMaxWeight()) {
                entity.setUserId(message.getUserId());
                entity.setEventId(message.getEventId());
                entity.setMaxWeight(weight);
                entity.setLastActionAt(message.getTimestamp());
                interactionRepository.save(entity);
                log.info("Сохранено: userId={}, eventId={}, weight={}",
                        message.getUserId(), message.getEventId(), weight);
            }

        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя: {}", e.getMessage(), e);
        }
    }

    private double getActionWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> ActionWeights.VIEW;
            case "REGISTER" -> ActionWeights.REGISTER;
            case "LIKE" -> ActionWeights.LIKE;
            default -> 0.0;
        };
    }
}