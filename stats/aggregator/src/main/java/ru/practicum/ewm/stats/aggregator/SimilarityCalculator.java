package ru.practicum.ewm.stats.aggregator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.dto.ActionWeights;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SimilarityCalculator {

    @Getter
    private final Map<Long, Map<Long, Double>> userWeights = new HashMap<>();
    private final Map<Long, Double> eventSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public double getCosineSimilarity(long eventA, long eventB) {
        return calculateCosineSimilarity(eventA, eventB);
    }

    public void processAction(long userId, long eventId, double weight) {
        log.info("Processing action: userId={}, eventId={}, weight={}", userId, eventId, weight);

        Map<Long, Double> eventUsers = userWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double currentWeight = eventUsers.get(userId);

        if (currentWeight != null && currentWeight >= weight) {
            return;
        }

        double weightDiff = weight - (currentWeight != null ? currentWeight : 0.0);
        eventUsers.put(userId, weight);

        eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weightDiff);

        recalculateSimilarities(eventId, userId, weightDiff);
    }

    private void recalculateSimilarities(long updatedEventId, long userId, double weightDiff) {
        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(updatedEventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);
            Double otherWeight = otherEventUsers.get(userId);

            if (otherWeight != null) {
                double oldMin = Math.min(
                        userWeights.get(updatedEventId).get(userId) - weightDiff,
                        otherWeight
                );
                double newMin = Math.min(
                        userWeights.get(updatedEventId).get(userId),
                        otherWeight
                );

                putMinSum(updatedEventId, otherEventId,
                        getMinSum(updatedEventId, otherEventId) - oldMin + newMin);

                double similarity = calculateCosineSimilarity(updatedEventId, otherEventId);
                log.debug("Similarity updated: eventA={}, eventB={}, similarity={}",
                        updatedEventId, otherEventId, similarity);
            } else {
                log.debug("Пользователь {} не взаимодействовал с событием {}, minWeight=0",
                        userId, otherEventId);
            }
        }
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