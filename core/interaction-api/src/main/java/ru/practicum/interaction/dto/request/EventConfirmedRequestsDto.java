package ru.practicum.interaction.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventConfirmedRequestsDto {
    private Long eventId;
    private Long confirmedCount;
}