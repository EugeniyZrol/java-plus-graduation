package ru.practicum.compilation.mapper;

import ru.practicum.interaction.dto.compilation.CompilationResponse;
import ru.practicum.interaction.dto.compilation.NewCompilationRequest;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.interaction.dto.event.EventShortDto;
import org.mapstruct.*;

import java.util.Set;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationRequest request);

    @Mapping(target = "events", source = "events")
    CompilationResponse toDto(Compilation compilation, Set<EventShortDto> events);
}