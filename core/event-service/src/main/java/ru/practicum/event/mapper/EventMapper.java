package ru.practicum.event.mapper;

import ru.practicum.categories.service.CategoryService;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventLocation;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.*;
import ru.practicum.interaction.dto.user.UserShortDto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
@AllArgsConstructor
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class EventMapper {

    @Autowired
    private CategoryService categoryService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "location", source = "location")
    @Mapping(target = "isPaid", source = "paid")
    @Mapping(target = "isRequestModeration", source = "requestModeration")
    @Mapping(target = "state", ignore = true)
    public abstract Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "category", expression = "java(getCategoryDto(event.getCategoryId()))")
    @Mapping(target = "initiator", expression = "java(getUserShortDto(event.getInitiatorId()))")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "paid", source = "isPaid")
    @Mapping(target = "views", ignore = true)
    public abstract EventShortDto toShortDto(Event event);

    @Mapping(target = "category", expression = "java(getCategoryDto(event.getCategoryId()))")
    @Mapping(target = "initiator", expression = "java(getUserShortDto(event.getInitiatorId()))")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "paid", source = "isPaid")
    @Mapping(target = "requestModeration", source = "isRequestModeration")
    @Mapping(target = "createdOn", source = "createdAt")
    @Mapping(target = "publishedOn", source = "publishedAt")
    public abstract EventFullDto toFullDto(Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "isPaid", source = "paid")
    @Mapping(target = "isRequestModeration", source = "requestModeration")
    @Mapping(target = "state", ignore = true)
    public abstract void updateEventFromAdminRequest(UpdateEventAdminRequest request, @MappingTarget Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "isPaid", source = "paid")
    @Mapping(target = "isRequestModeration", source = "requestModeration")
    @Mapping(target = "state", ignore = true)
    public abstract void updateEventFromUserRequest(UpdateEventUserRequest request, @MappingTarget Event event);

    protected EventLocation mapLocationDtoToEventLocation(LocationDto locationDto) {
        if (locationDto == null) {
            return null;
        }
        return new EventLocation(
                locationDto.getLat(),
                locationDto.getLon()
        );
    }

    protected LocationDto mapEventLocationToLocationDto(EventLocation eventLocation) {
        if (eventLocation == null) {
            return null;
        }
        return new LocationDto(
                eventLocation.getLatitude(),
                eventLocation.getLongitude()
        );
    }

    protected CategoryDto getCategoryDto(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        try {
            return categoryService.getCategoryById(categoryId);
        } catch (Exception e) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(categoryId);
            categoryDto.setName("Unknown Category");
            return categoryDto;
        }
    }

    protected UserShortDto getUserShortDto(Long userId) {
        if (userId == null) {
            return null;
        }
        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(userId);
        userShortDto.setName("Unknown User");
        return userShortDto;
    }
}