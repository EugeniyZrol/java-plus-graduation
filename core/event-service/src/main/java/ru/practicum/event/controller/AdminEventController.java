package ru.practicum.event.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.interaction.dto.event.AdminEventSearchRequest;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.UpdateEventAdminRequest;
import ru.practicum.event.service.AdminEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    public List<EventFullDto> getEvents(@RequestParam(required = false) List<Long> users,
                                        @RequestParam(required = false) List<String> states,
                                        @RequestParam(required = false) List<Long> categories,
                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        AdminEventSearchRequest request = AdminEventSearchRequest.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();
        Pageable pageable = PageRequest.of(from / size, size);
        return adminEventService.getEvents(request, pageable);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest request) {
        return adminEventService.updateEvent(eventId, request);
    }
}