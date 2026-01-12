package ewm.event.controller;

import ewm.event.dto.AdminEventSearchRequest;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventAdminRequest;
import ewm.event.service.AdminEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    public List<EventFullDto> getEvents(@ModelAttribute @Valid AdminEventSearchRequest request) {
        int size = request.getSize() != null ? Math.max(1, request.getSize()) : 10;
        int from = request.getFrom() != null ? request.getFrom() : 0;
        int page = from / size;

        PageRequest pageRequest = PageRequest.of(page, size);

        return adminEventService.getEvents(request, pageRequest);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @RequestBody @Valid UpdateEventAdminRequest request) {
        return adminEventService.updateEvent(eventId, request);
    }
}