package ewm.event.service;

import ewm.event.dto.AdminEventSearchRequest;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventAdminRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminEventService {
    List<EventFullDto> getEvents(AdminEventSearchRequest requestParams, Pageable pageable);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request);
}
