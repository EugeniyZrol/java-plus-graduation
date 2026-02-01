package ru.practicum.ewm.stats.analyzer.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.UserInteractionEntity;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends
        RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Получение рекомендаций для пользователя: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());

            List<RecommendedEventProto> recommendations =
                    getRecommendations(request.getUserId(), request.getMaxResults());

            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Поиск похожих событий: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            List<RecommendedEventProto> similarEvents =
                    getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());

            similarEvents.forEach(responseObserver::onNext);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            request.getEventIdList().forEach(eventId -> {
                Double totalWeight = interactionRepository.sumWeightsByEventId(eventId);

                RecommendedEventProto response = RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(totalWeight != null ? totalWeight : 0.0)
                        .build();
                responseObserver.onNext(response);
            });
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    private List<RecommendedEventProto> getRecommendations(long userId, int maxResults) {
        Pageable recentLimit = PageRequest.of(0, 10);
        List<Long> recentEventIds = interactionRepository
                .findRecentInteractions(userId, recentLimit)
                .stream()
                .map(UserInteractionEntity::getEventId)
                .toList();

        if (recentEventIds.isEmpty()) {
            return List.of();
        }

        List<Long> userEventIds = interactionRepository.findEventIdsByUserId(userId);

        Pageable similarityLimit = PageRequest.of(0, maxResults * 2);
        List<EventSimilarityEntity> similarities = similarityRepository
                .findSimilarEvents(recentEventIds.getFirst(), similarityLimit);

        return similarities.stream()
                .filter(sim -> !userEventIds.contains(getOtherEventId(sim, recentEventIds.getFirst())))
                .limit(maxResults)
                .map(sim -> RecommendedEventProto.newBuilder()
                        .setEventId(getOtherEventId(sim, recentEventIds.getFirst()))
                        .setScore(sim.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<Long> userEventIds = interactionRepository.findEventIdsByUserId(userId);
        Pageable limit = PageRequest.of(0, maxResults);
        List<EventSimilarityEntity> similarities = similarityRepository.findSimilarEvents(eventId, limit);

        return similarities.stream()
                .filter(sim -> {
                    long otherEventId = getOtherEventId(sim, eventId);
                    return !userEventIds.contains(otherEventId);
                })
                .map(sim -> RecommendedEventProto.newBuilder()
                        .setEventId(getOtherEventId(sim, eventId))
                        .setScore(sim.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private long getOtherEventId(EventSimilarityEntity similarity, long knownEventId) {
        return similarity.getEventA().equals(knownEventId)
                ? similarity.getEventB()
                : similarity.getEventA();
    }
}