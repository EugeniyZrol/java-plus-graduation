package model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHitDto {

    @NotBlank(message = "App cannot be blank")
    private String app;

    @NotBlank(message = "Uri cannot be blank")
    private String uri;

    @NotBlank(message = "Ip cannot be blank")
    private String ip;

    @NotNull(message = "Timestamp cannot be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}