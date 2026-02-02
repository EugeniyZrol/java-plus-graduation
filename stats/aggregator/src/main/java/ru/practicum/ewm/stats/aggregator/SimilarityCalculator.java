package ru.practicum.ewm.stats.aggregator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.dto.ActionWeights;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class SimilarityCalculator {

    @Getter
    private final Map<Long, Map<Long, Double>> userWeights = new HashMap<>();
    private final Map<Long, Double> eventSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public List<EventSimilarityAvro> processAction(long userId, long eventId, double weight, Instant timestamp) {
        log.info("Processing action: userId={}, eventId={}, weight={}", userId, eventId, weight);

        // 1. Получаем или создаём мапу пользователей для события
        Map<Long, Double> eventUsers = userWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = eventUsers.get(userId);

        // 2. Проверяем нужно ли обновление
        if (oldWeight != null && oldWeight >= weight) {
            log.debug("Вес не увеличился: текущий={}, новый={}", oldWeight, weight);
            return Collections.emptyList();
        }

        // 3. Обновляем вес пользователя
        double weightDiff = weight - (oldWeight != null ? oldWeight : 0.0);
        eventUsers.put(userId, weight);

        // 4. Обновляем сумму весов события
        eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weightDiff);

        List<EventSimilarityAvro> messages = new ArrayList<>();

        // 5. Для каждого другого события
        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);

            // 6. Если пользователь взаимодействовал с обоими событиями
            if (otherEventUsers.containsKey(userId)) {
                double otherWeight = otherEventUsers.get(userId);

                // 7. Считаем изменение sMin только для этого пользователя
                double oldMin = Math.min(oldWeight != null ? oldWeight : 0.0, otherWeight);
                double newMin = Math.min(weight, otherWeight);
                double sMinDiff = newMin - oldMin;

                if (sMinDiff != 0.0) {
                    double oldSmin = getMinSum(eventId, otherEventId);
                    double newSmin = oldSmin + sMinDiff;
                    putMinSum(eventId, otherEventId, newSmin);

                    // 8. Считаем сходство
                    double similarity = calculateCosineSimilarity(eventId, otherEventId);

                    if (similarity > 0.0) {
                        long first = Math.min(eventId, otherEventId);
                        long second = Math.max(eventId, otherEventId);

                        EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                                .setEventA(first)
                                .setEventB(second)
                                .setScore(similarity)
                                .setTimestamp(timestamp)
                                .build();
                        messages.add(message);

                        log.info("Сходство: {}<->{} = {} (oldMin={}, newMin={}, sMinDiff={})",
                                first, second, similarity, oldMin, newMin, sMinDiff);
                    }
                }
            }
        }

        return messages;
    }

    private List<EventSimilarityAvro> handleNewEvent(long userId, long eventId, double weight, Instant timestamp) {
        log.info("Первое взаимодействие с мероприятием: eventId={}, userId={}, weight={}",
                eventId, userId, weight);

        Map<Long, Double> eventUsers = new HashMap<>();
        eventUsers.put(userId, weight);
        userWeights.put(eventId, eventUsers);
        eventSums.put(eventId, weight);

        List<EventSimilarityAvro> messages = new ArrayList<>();

        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);

            if (otherEventUsers.containsKey(userId)) {
                double otherWeight = otherEventUsers.get(userId);
                double sMin = Math.min(weight, otherWeight);

                putMinSum(eventId, otherEventId, sMin);
                double similarity = calculateCosineSimilarity(eventId, otherEventId);

                if (similarity > 0.0) {
                    long first = Math.min(eventId, otherEventId);
                    long second = Math.max(eventId, otherEventId);

                    EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                            .setEventA(first)
                            .setEventB(second)
                            .setScore(similarity)
                            .setTimestamp(timestamp)
                            .build();
                    messages.add(message);

                    log.debug("Новое сходство: eventA={}, eventB={}, score={}",
                            first, second, similarity);
                }
            }
        }

        return messages;
    }

    private double calculateCosineSimilarity(long eventA, long eventB) {
        double sMin = getMinSum(eventA, eventB);
        double sA = eventSums.getOrDefault(eventA, 0.0);
        double sB = eventSums.getOrDefault(eventB, 0.0);

        if (sA == 0.0 || sB == 0.0) {
            return 0.0;
        }

        return sMin / Math.sqrt(sA * sB);
    }

    private void putMinSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .put(second, sum);
    }

    private double getMinSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSums
                .getOrDefault(first, new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    public double getActionWeight(String actionType) {
        String normalizedType = actionType.replace("ACTION_", "");
        return switch (normalizedType) {
            case "VIEW" -> ActionWeights.VIEW;      // 0.4
            case "REGISTER" -> ActionWeights.REGISTER; // 0.8
            case "LIKE" -> ActionWeights.LIKE;      // 1.2
            default -> 0.0;
        };
    }
}