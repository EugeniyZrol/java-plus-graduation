package ewm.event.dto;

import ewm.event.model.EventSort;
import ewm.util.validation.ValidEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicEventSearchRequest {

    private String text;

    private List<Long> categories;

    private Boolean paid;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeEnd;

    private Boolean onlyAvailable;

    @ValidEnum(
            enumClass = EventSort.class,
            values = { "EVENT_DATE", "VIEWS" },
            message = "Недопустимое значение. Допустимые: {accepted}"
    )
    private String sort;

    @Builder.Default
    private Integer from = 0;

    @Builder.Default
    private Integer size = 10;
}