package ewm.compilation.mapper;

import ewm.compilation.dto.CompilationResponse;
import ewm.compilation.dto.NewCompilationRequest;
import ewm.compilation.model.Compilation;
import ewm.event.dto.EventShortDto;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationRequest request);

    @Mapping(target = "events", source = "events", qualifiedByName = "eventsToShortDtos")
    CompilationResponse toDto(Compilation compilation);

    @Named("eventsToShortDtos")
    default Set<EventShortDto> eventsToShortDtos(Set<Event> events) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .map(EventMapper.INSTANCE::toShortDto)
                .collect(Collectors.toSet());
    }
}