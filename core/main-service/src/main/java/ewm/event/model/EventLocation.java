package ewm.event.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventLocation {
    @NotNull
    private Float latitude;
    @NotNull
    private Float longitude;
}
