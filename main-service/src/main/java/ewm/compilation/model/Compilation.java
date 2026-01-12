package ewm.compilation.model;

import ewm.event.model.Event;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private Set<Event> events;
}