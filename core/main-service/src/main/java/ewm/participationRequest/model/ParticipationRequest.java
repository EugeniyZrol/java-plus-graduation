package ewm.participationRequest.model;

import ewm.event.model.Event;
import ewm.user.model.User;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne
    @JoinColumn (name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn (name = "requester_id")
    private User requester;

    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}
