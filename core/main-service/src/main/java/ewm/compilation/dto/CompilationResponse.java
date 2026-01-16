package ewm.compilation.dto;

import ewm.event.dto.EventShortDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationResponse {
    private Long id;
    private String title;
    private Boolean pinned;
    private Set<EventShortDto> events;
}
