package ru.practicum.interaction.client.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.interaction.client.feign.EventClient;
import ru.practicum.interaction.dto.event.EventFullDto;

@Component
public class EventClientFallback implements EventClient {
    @Override public Boolean existsEventById(Long eventId) { return true; }
    @Override public String getEventState(Long eventId) { return "PUBLISHED"; }
    @Override public EventFullDto getEventById(Long eventId) { return null; }
}