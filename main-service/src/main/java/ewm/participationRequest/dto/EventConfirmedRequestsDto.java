package ewm.participationRequest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventConfirmedRequestsDto {
    private Long eventId;
    private Long confirmedCount;
}