package ru.practicum.ewm.stats.collector;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import com.google.protobuf.Empty;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;
    private static final String TOPIC = "stats.user-actions.v1";

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено пользовательское действие: userId={}, eventId={}, action={}",
                    request.getUserId(), request.getEventId(), request.getActionType());

            Instant instant = Instant.ofEpochSecond(
                    request.getTimestamp().getSeconds(),
                    request.getTimestamp().getNanos()
            );

            UserActionAvro avroMessage = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(mapActionType(request.getActionType()))
                    .setTimestamp(instant)
                    .build();

            log.debug("Отправляю в Kafka топик {}: {}", TOPIC, avroMessage);

            kafkaTemplate.send(TOPIC, avroMessage).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Сообщение успешно отправлено в Kafka: {}", result);
                    responseObserver.onNext(Empty.getDefaultInstance());
                    responseObserver.onCompleted();
                } else {
                    log.error("Ошибка при отправке в Kafka", ex);
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Failed to send to Kafka: " + ex.getMessage())
                            .asRuntimeException());
                }
            });

        } catch (Exception e) {
            log.error("Ошибка при обработке пользовательского действия", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private ActionTypeAvro mapActionType(ru.practicum.ewm.stats.proto.ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> {
                log.warn("Неизвестный тип действия: {}, используем VIEW по умолчанию", protoType);
                yield ActionTypeAvro.VIEW;
            }
        };
    }
}