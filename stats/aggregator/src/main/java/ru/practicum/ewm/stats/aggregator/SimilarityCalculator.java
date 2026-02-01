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
    @Getter
    private final Set<Long> updatedEvents = new HashSet<>();

    public double getCosineSimilarity(long eventA, long eventB) {
        return calculateCosineSimilarity(eventA, eventB);
    }

    public List<EventSimilarityAvro> processAction(long userId, long eventId, double weight, Instant timestamp) {
        log.info("Processing action: userId={}, eventId={}, weight={}", userId, eventId, weight);

        Map<Long, Double> eventUsers = userWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double currentWeight = eventUsers.get(userId);

        if (currentWeight != null && currentWeight >= weight) {
            log.debug("Вес не увеличился: текущий={}, новый={}", currentWeight, weight);
            return Collections.emptyList();
        }

        double weightDiff = weight - (currentWeight != null ? currentWeight : 0.0);
        eventUsers.put(userId, weight);

        eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weightDiff);

        List<EventSimilarityAvro> messages = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : userWeights.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = entry.getValue();
            if (!otherEventUsers.containsKey(userId)) continue;

            double otherWeight = otherEventUsers.get(userId);
            double oldWeightForEvent = userWeights.get(eventId).get(userId) - weightDiff;

            double oldMin = Math.min(oldWeightForEvent, otherWeight);
            double newMin = Math.min(userWeights.get(eventId).get(userId), otherWeight);

            putMinSum(eventId, otherEventId,
                    getMinSum(eventId, otherEventId) - oldMin + newMin);

            double similarity = calculateCosineSimilarity(eventId, otherEventId);

            if (similarity > 0.01) {
                long first = Math.min(eventId, otherEventId);
                long second = Math.max(eventId, otherEventId);

                EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                        .setEventA(first)
                        .setEventB(second)
                        .setScore(similarity)
                        .setTimestamp(timestamp)
                        .build();
                messages.add(message);
            }
        }

        return messages;
    }

    public void clearUpdatedEvents() {
        updatedEvents.clear();
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
        return switch (actionType) {
            case "VIEW" -> ActionWeights.VIEW;
            case "REGISTER" -> ActionWeights.REGISTER;
            case "LIKE" -> ActionWeights.LIKE;
            default -> 0.0;
        };
    }

    public boolean shouldSendSimilarity(long eventA, long eventB, double similarity) {
        return similarity > 0.01;
    }
}