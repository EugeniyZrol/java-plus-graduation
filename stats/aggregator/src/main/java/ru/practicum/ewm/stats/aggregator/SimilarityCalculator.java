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

        Map<Long, Double> eventUsers = userWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = eventUsers.get(userId);

        if (oldWeight != null && oldWeight >= weight) {
            log.debug("Вес не увеличился: текущий={}, новый={}", oldWeight, weight);
            return Collections.emptyList();
        }

        List<EventSimilarityAvro> messages = new ArrayList<>();

        if (oldWeight == null) {
            eventUsers.put(userId, weight);
            eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weight);
        } else {
            eventSums.put(eventId, eventSums.get(eventId) - oldWeight + weight);
            eventUsers.put(userId, weight);
        }

        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);

            if (otherEventUsers.containsKey(userId)) {
                double otherWeight = otherEventUsers.get(userId);
                double oldMin = oldWeight != null ? Math.min(oldWeight, otherWeight) : 0.0;
                double newMin = Math.min(weight, otherWeight);
                double sMinDiff = newMin - oldMin;

                double currentSmin = getMinSum(eventId, otherEventId);
                double newSmin = currentSmin + sMinDiff;
                putMinSum(eventId, otherEventId, newSmin);

                double similarity = calculateCosineSimilarity(eventId, otherEventId);
                similarity = Math.round(similarity * 100.0) / 100.0;

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

                    log.info("Обновлено сходство: {}<->{} = {} (oldWeight={}, weight={}, otherWeight={})",
                            first, second, similarity, oldWeight, weight, otherWeight);
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

        double similarity = sMin / Math.sqrt(sA * sB);
        return Math.round(similarity * 100.0) / 100.0;
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