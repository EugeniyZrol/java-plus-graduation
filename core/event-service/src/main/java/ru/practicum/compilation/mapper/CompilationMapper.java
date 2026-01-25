package ru.practicum.compilation.mapper;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.interaction.dto.compilation.CompilationResponse;
import ru.practicum.interaction.dto.compilation.NewCompilationRequest;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
@AllArgsConstructor
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {EventMapper.class}
)
public abstract class CompilationMapper {

    @Autowired
    private EventMapper eventMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    public abstract Compilation toEntity(NewCompilationRequest request);

    @Mapping(target = "events", source = "events", qualifiedByName = "eventsToShortDtos")
    public abstract CompilationResponse toDto(Compilation compilation);

    @Named("eventsToShortDtos")
    protected Set<EventShortDto> eventsToShortDtos(Set<Event> events) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toSet());
    }
}