package ru.practicum.event.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.interaction.enums.event.EventState;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "published_on")
    private LocalDateTime publishedAt;

    @Embedded
    private EventLocation location;

    @Column(name = "paid", nullable = false)
    @Builder.Default
    private Boolean isPaid = false;

    @Column(name = "participant_limit", nullable = false)
    @Builder.Default
    private Integer participantLimit = 0;

    @Column(name = "request_moderation", nullable = false)
    @Builder.Default
    private Boolean isRequestModeration = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventState state;
}