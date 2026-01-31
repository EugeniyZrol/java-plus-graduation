package ru.practicum.event.mapper;

import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventLocation;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.*;
import ru.practicum.interaction.dto.user.UserShortDto;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryId", source = "newEventDto.category")
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "location", source = "newEventDto.location")
    @Mapping(target = "isPaid", source = "newEventDto.paid")
    @Mapping(target = "isRequestModeration", source = "newEventDto.requestModeration")
    @Mapping(target = "state", ignore = true)
    Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "paid", source = "event.isPaid")
    @Mapping(target = "rating", ignore = true)
    EventShortDto toShortDto(Event event, CategoryDto category, UserShortDto initiator);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "paid", source = "event.isPaid")
    @Mapping(target = "requestModeration", source = "event.isRequestModeration")
    @Mapping(target = "createdOn", source = "event.createdAt")
    @Mapping(target = "publishedOn", source = "event.publishedAt")
    EventFullDto toFullDto(Event event, CategoryDto category, UserShortDto initiator);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryId", source = "request.category")
    @Mapping(target = "location", source = "request.location")
    @Mapping(target = "isPaid", source = "request.paid")
    @Mapping(target = "isRequestModeration", source = "request.requestModeration")
    @Mapping(target = "state", ignore = true)
    void updateEventFromAdminRequest(UpdateEventAdminRequest request, @MappingTarget Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryId", source = "request.category")
    @Mapping(target = "location", source = "request.location")
    @Mapping(target = "isPaid", source = "request.paid")
    @Mapping(target = "isRequestModeration", source = "request.requestModeration")
    @Mapping(target = "state", ignore = true)
    void updateEventFromUserRequest(UpdateEventUserRequest request, @MappingTarget Event event);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "paid", source = "isPaid")
    EventShortDto toShortDtoWithoutRelations(Event event);

    default EventLocation mapLocationDtoToEventLocation(LocationDto locationDto) {
        if (locationDto == null) {
            return null;
        }
        return new EventLocation(locationDto.getLat(), locationDto.getLon());
    }

    default LocationDto mapEventLocationToLocationDto(EventLocation eventLocation) {
        if (eventLocation == null) {
            return null;
        }
        return new LocationDto(eventLocation.getLatitude(), eventLocation.getLongitude());
    }
}