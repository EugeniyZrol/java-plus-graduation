package ru.practicum.interaction.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.dto.event.EventShortDto;

import java.util.List;
import java.util.Set;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/events/feign/{eventId}/exists")
    Boolean existsEventById(@PathVariable Long eventId);

    @GetMapping("/events/feign/{eventId}/initiator")
    Long getEventInitiatorId(@PathVariable Long eventId);

    @GetMapping("/events/feign/{eventId}/state")
    String getEventState(@PathVariable Long eventId);

    @GetMapping("/events/feign/{eventId}/participant-limit")
    Integer getParticipantLimit(@PathVariable Long eventId);

    @GetMapping("/events/feign/{eventId}/moderation")
    Boolean isRequestModerationEnabled(@PathVariable Long eventId);
}