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
        Map<Long, Double> eventUsers = userWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = eventUsers.get(userId);

        if (oldWeight != null && oldWeight >= weight) {
            return Collections.emptyList();
        }

        double weightDiff = weight - (oldWeight != null ? oldWeight : 0.0);
        eventUsers.put(userId, weight);
        eventSums.put(eventId, eventSums.getOrDefault(eventId, 0.0) + weightDiff);

        List<EventSimilarityAvro> messages = new ArrayList<>();

        for (Long otherEventId : userWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Double> otherEventUsers = userWeights.get(otherEventId);
            double sMinDiff = 0.0;

            for (Long uid : eventUsers.keySet()) {
                if (otherEventUsers.containsKey(uid)) {
                    double currentMin = Math.min(eventUsers.get(uid), otherEventUsers.get(uid));

                    if (uid.equals(userId)) {
                        double oldMin = Math.min(oldWeight != null ? oldWeight : 0.0, otherEventUsers.get(uid));
                        sMinDiff += currentMin - oldMin;
                    }
                }
            }

            if (sMinDiff != 0.0) {
                double oldSmin = getMinSum(eventId, otherEventId);
                putMinSum(eventId, otherEventId, oldSmin + sMinDiff);

                double similarity = calculateCosineSimilarity(eventId, otherEventId);
                if (similarity > 0.00) {
                    long first = Math.min(eventId, otherEventId);
                    long second = Math.max(eventId, otherEventId);

                    messages.add(EventSimilarityAvro.newBuilder()
                            .setEventA(first)
                            .setEventB(second)
                            .setScore(similarity)
                            .setTimestamp(timestamp)
                            .build());
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
        return switch (actionType) {
            case "VIEW" -> ActionWeights.VIEW;
            case "REGISTER" -> ActionWeights.REGISTER;
            case "LIKE" -> ActionWeights.LIKE;
            default -> 0.0;
        };
    }
}