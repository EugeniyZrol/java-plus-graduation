package ewm.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    @NotNull
    @Min(-90)
    @Max(90)
    private Float lat;

    @NotNull
    @Min(-180)
    @Max(180)
    private Float lon;
}
