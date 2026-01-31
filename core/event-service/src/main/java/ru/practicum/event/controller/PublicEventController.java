package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.model.Event;
import ru.practicum.ewm.stats.client.AnalyzerGrpcClient;
import ru.practicum.ewm.stats.model.RecommendedEvent;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.event.PublicEventSearchRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.interaction.enums.event.EventState;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventController {
    private final EventService eventService;
    private final AnalyzerGrpcClient analyzerGrpcClient;

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                         @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(defaultValue = "0") @Min(0) int from,
                                         @RequestParam(defaultValue = "10") @Min(1) int size,
                                         HttpServletRequest request) {
        PublicEventSearchRequest params = PublicEventSearchRequest.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .build();
        Pageable pageable = PageRequest.of(from / size, size);
        return eventService.getPublicEvents(params, pageable, request.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id,
                                 @RequestHeader("X-EWM-USER-ID") Long userId,
                                 HttpServletRequest request) {
        return eventService.getPublicEventById(id, userId, request.getRemoteAddr());
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        return eventService.getRecommendations(userId, size);
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @PathVariable Long eventId) {
        eventService.likeEvent(userId, eventId);
        return ResponseEntity.ok().build();
    }
}