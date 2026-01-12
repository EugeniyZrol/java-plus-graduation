package ewm.event.mapper;

import ewm.categories.dto.CategoryDto;
import ewm.categories.model.Category;
import ewm.event.dto.*;
import ewm.event.model.Event;
import ewm.event.model.EventLocation;
import ewm.user.dto.UserShortDto;
import ewm.user.model.User;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EventMapper {
    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryIdToCategory")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt",  ignore = true)
    @Mapping(target = "location", source = "location", qualifiedByName = "locationDtoToEventLocation")
    @Mapping(target = "isPaid", source = "paid")
    @Mapping(target = "isRequestModeration", source = "requestModeration")
    Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "category", source = "category", qualifiedByName = "categoryToDto")
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = "userToShortDto")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "paid", source = "isPaid")
    @Mapping(target = "views", ignore = true)
    EventShortDto toShortDto(Event event);

    @Mapping(target = "annotation", source = "annotation")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "eventDate", source = "eventDate")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "participantLimit", source = "participantLimit")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "createdOn", source = "createdAt")
    @Mapping(target = "publishedOn", source = "publishedAt")
    @Mapping(target = "paid", source = "isPaid")
    @Mapping(target = "requestModeration", source = "isRequestModeration")
    @Mapping(target = "location", source = "location", qualifiedByName = "eventLocationToLocationDto")
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryToDto")
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = "userToShortDto")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventFullDto toFullDto(Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryIdToCategory")
    @Mapping(target = "location", source = "location", qualifiedByName = "locationDtoToEventLocation")
    @Mapping(target = "isPaid", source = "paid")
    @Mapping(target = "isRequestModeration", source = "requestModeration")
    @Mapping(target = "state", ignore = true)
    void updateEventFromAdminRequest(UpdateEventAdminRequest request, @MappingTarget Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryIdToCategory")
    @Mapping(target = "location", source = "location", qualifiedByName = "locationDtoToEventLocation")
    @Mapping(target = "isPaid", source = "paid")
    @Mapping(target = "isRequestModeration", source = "requestModeration")
    @Mapping(target = "state", ignore = true)
    void updateEventFromUserRequest(UpdateEventUserRequest request, @MappingTarget Event event);

    @Named("categoryIdToCategory")
    default Category toCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return new Category(categoryId, null);
    }

    @Named("categoryToDto")
    default CategoryDto categoryToDto(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());
        return categoryDto;
    }

    @Named("userToShortDto")
    default UserShortDto userToShortDto(User user) {
        if (user == null) {
            return null;
        }
        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(user.getId());
        userShortDto.setName(user.getName());
        return userShortDto;
    }

    @Named("locationDtoToEventLocation")
    default EventLocation toEventLocation(LocationDto locationDto) {
        if (locationDto == null) {
            return null;
        }
        return new EventLocation(
                locationDto.getLat(),
                locationDto.getLon()
        );
    }

    @Named("eventLocationToLocationDto")
    default LocationDto toLocationDto(EventLocation eventLocation) {
        if (eventLocation == null) {
            return null;
        }
        return new LocationDto(
                eventLocation.getLatitude(),
                eventLocation.getLongitude()
        );
    }
}