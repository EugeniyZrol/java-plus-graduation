package ru.practicum.event.service;

import ru.practicum.interaction.dto.event.AdminEventSearchRequest;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.UpdateEventAdminRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminEventService {
    List<EventFullDto> getEvents(AdminEventSearchRequest requestParams, Pageable pageable);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request);
}
