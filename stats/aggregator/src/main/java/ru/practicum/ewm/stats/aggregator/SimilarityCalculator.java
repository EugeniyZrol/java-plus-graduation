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

        // 1. Проверяем, существует ли уже взаимодействие пользователя с событием
        Map<Long, Double> eventUsers = userWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = eventUsers.get(userId);

        // 2. Согласно ТЗ: "когда один пользователь совершает несколько действий с одним и тем же мероприятием,
        // учитывается только действие с максимальным весом"
        if (oldWeight != null && oldWeight >= weight) {
            log.debug("Вес не увеличился: текущий={}, новый={}", oldWeight, weight);
            return Collections.emptyList(); // Вес не увеличился - не нужно пересчитывать
        }

        List<EventSimilarityAvro> messages = new ArrayList<>();

        // 3. Обновляем вес пользователя для этого события
        double weightDiff;
        if (oldWeight == null) {
            // Первое взаимодействие пользователя с этим событием
            weightDiff = weight;
            eventUsers.put(userId, weight);

            // Обновляем сумму весов события
            eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weight);

            // 4. Обрабатываем как новое событие для этого пользователя
            return handleNewEvent(userId, eventId, weight, timestamp);
        } else {
            // Увеличиваем вес пользователя
            weightDiff = weight - oldWeight;
            eventUsers.put(userId, weight);

            // Обновляем сумму весов события
            eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weightDiff);
        }

        // 5. Для каждого другого события, с которым взаимодействовал этот пользователь
        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);

            // Если пользователь взаимодействовал с обоими событиями
            if (otherEventUsers.containsKey(userId)) {
                double otherWeight = otherEventUsers.get(userId);

                // 6. Обновляем сумму минимальных весов для этой пары событий
                double oldMin = Math.min(oldWeight, otherWeight);
                double newMin = Math.min(weight, otherWeight);
                double sMinDiff = newMin - oldMin;

                if (Math.abs(sMinDiff) > 0.000001) { // Учитываем погрешность double
                    double currentSmin = getMinSum(eventId, otherEventId);
                    double newSmin = currentSmin + sMinDiff;
                    putMinSum(eventId, otherEventId, newSmin);

                    // 7. Пересчитываем сходство
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

                        log.info("Обновлено сходство: {}<->{} = {} (oldWeight={}, newWeight={}, otherWeight={})",
                                first, second, similarity, oldWeight, weight, otherWeight);
                    }
                }
            }
        }

        return messages;
    }

    private List<EventSimilarityAvro> handleNewEvent(long userId, long eventId, double weight, Instant timestamp) {
        log.info("Первое взаимодействие пользователя {} с мероприятием {}: weight={}",
                userId, eventId, weight);

        List<EventSimilarityAvro> messages = new ArrayList<>();

        // Для каждого другого события, с которым взаимодействовал этот пользователь
        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);

            if (otherEventUsers.containsKey(userId)) {
                double otherWeight = otherEventUsers.get(userId);

                // Добавляем минимальный вес для этой пары
                double minWeight = Math.min(weight, otherWeight);
                double currentSmin = getMinSum(eventId, otherEventId);
                double newSmin = currentSmin + minWeight;
                putMinSum(eventId, otherEventId, newSmin);

                // Пересчитываем сходство
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

        if (sA == 0.0 || sB == 0.0 || sMin == 0.0) {
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
            case "VIEW" -> ActionWeights.VIEW;
            case "REGISTER" -> ActionWeights.REGISTER;
            case "LIKE" -> ActionWeights.LIKE;
            default -> 0.0;
        };
    }
}