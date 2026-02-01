package ru.practicum.ewm.stats.client;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;
import ru.practicum.ewm.stats.model.RecommendedEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);

            List<RecommendedEvent> result = new ArrayList<>();
            while (iterator.hasNext()) {
                RecommendedEventProto proto = iterator.next();
                result.add(new RecommendedEvent(proto.getEventId(), proto.getScore()));
            }

            return result;

        } catch (StatusRuntimeException e) {
            log.error("Не удалось получить рекомендации: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            var iterator = analyzerStub.getSimilarEvents(request);

            return StreamSupport.stream(((Iterable<RecommendedEventProto>) () -> iterator).spliterator(), false)
                    .map(proto -> new RecommendedEvent(proto.getEventId(), proto.getScore()))
                    .collect(Collectors.toList());

        } catch (StatusRuntimeException e) {
            log.error("Не удалось получить похожие события: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public double getEventRating(long eventId) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addEventId(eventId)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);

            if (iterator.hasNext()) {
                RecommendedEventProto result = iterator.next();
                return result.getScore();
            }

            return 0.0;

        } catch (StatusRuntimeException e) {
            log.error("Не удалось получить рейтинг события: {}", e.getMessage(), e);
            return 0.0;
        }
    }
}