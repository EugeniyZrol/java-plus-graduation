package ru.practicum.participationRequest.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.interaction.enums.request.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Table (name = "participation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "requester_id")
    private Long requesterId;

    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}
