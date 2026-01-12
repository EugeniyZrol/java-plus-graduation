package ewm.event.controller;

import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.PublicEventSearchRequest;
import ewm.event.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(@ModelAttribute @Valid PublicEventSearchRequest requestParams,
                                         HttpServletRequest request) {
        int size = requestParams.getSize() != null ? Math.max(1, requestParams.getSize()) : 10;
        int from = requestParams.getFrom() != null ? requestParams.getFrom() : 0;
        int page = from / size;

        PageRequest pageRequest = PageRequest.of(page, size);
        List<EventShortDto> events = eventService.getPublicEvents(requestParams, pageRequest, request.getRemoteAddr());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEvent(@PathVariable Long id, HttpServletRequest request) {
        EventFullDto event = eventService.getPublicEventById(id, request.getRemoteAddr());
        return ResponseEntity.ok(event);
    }
}