package ru.practicum.interaction.validation;

import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.enums.event.EventState;
import ru.practicum.interaction.dto.event.EventValidationInfo;

import java.time.LocalDateTime;

public class EventValidationUtils {

    public static void validateEventDate(LocalDateTime eventDate, long minHoursBefore) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(minHoursBefore))) {
            throw new ConflictException(
                    String.format("Дата начала события должна быть не ранее чем через %d часа(ов) от текущего момента",
                            minHoursBefore)
            );
        }
    }

    public static void validateDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Начальная дата не может быть позже конечной даты");
        }
    }

    public static void validateEventStateForUpdate(EventValidationInfo eventInfo) {
        if (eventInfo == null || eventInfo.getState() == null) {
            return;
        }

        EventState currentState = EventState.valueOf(eventInfo.getState());
        if (currentState == EventState.PUBLISHED) {
            throw new ConflictException(
                    "Изменить можно только отмененные события или события в состоянии ожидания модерации"
            );
        }
    }

    public static void validateParticipantLimit(Integer participantLimit) {
        if (participantLimit != null && participantLimit < 0) {
            throw new IllegalArgumentException("Лимит участников не может быть отрицательным");
        }
    }
}