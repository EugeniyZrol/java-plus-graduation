package ru.practicum.ewm.stats.analyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "user_interaction", indexes = {
        @Index(name = "idx_user_event", columnList = "userId, eventId", unique = true),
        @Index(name = "idx_user_last_action", columnList = "userId, lastActionAt DESC")
})
public class UserInteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "max_weight", nullable = false)
    private Double maxWeight;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInteractionEntity that = (UserInteractionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}