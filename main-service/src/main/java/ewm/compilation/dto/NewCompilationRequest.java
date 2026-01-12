package ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationRequest {

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;

    @Builder.Default
    private Boolean pinned = false;

    private Set<Long> events;
}
