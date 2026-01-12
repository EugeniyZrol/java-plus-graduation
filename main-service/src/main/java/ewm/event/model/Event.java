package ewm.event.model;

import ewm.categories.model.Category;
import ewm.user.model.User;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "published_on")
    private LocalDateTime publishedAt;

    //Сделал Embedded согласно замечанию. Однако мне не понятно, почему некорректно PGpoint использовать?
    // тип point как раз для работы с координатами, ранее использованное решение полностью работоспособно
    // просьба пояснить, почему именно это решение является верным
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "longitude"))
    })
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

    @Column(nullable = false, length = 20)
    private String state;

    public Boolean getRequestModeration() {
        return isRequestModeration != null ? isRequestModeration : true;
    }

    public Boolean getPaid() {
        return isPaid != null ? isPaid : false;
    }

    public Integer getParticipantLimit() {
        return participantLimit != null ? participantLimit : 0;
    }
}