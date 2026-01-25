package ru.practicum.interaction.client.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.interaction.client.feign.EventClient;

@Component
public class EventClientFallback implements EventClient {
    @Override public Boolean existsEventById(Long eventId) { return true; }
    @Override public Long getEventInitiatorId(Long eventId) { return 0L; }
    @Override public String getEventState(Long eventId) { return "PUBLISHED"; }
    @Override public Integer getParticipantLimit(Long eventId) { return 0; }
    @Override public Boolean isRequestModerationEnabled(Long eventId) { return false; }
}