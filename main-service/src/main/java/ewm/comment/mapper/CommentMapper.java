package ewm.comment.mapper;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.NewCommentDto;
import ewm.comment.dto.UpdateCommentDto;
import ewm.comment.model.Comment;
import ewm.event.model.Event;
import ewm.user.model.User;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isEdited", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Comment toComment(NewCommentDto newCommentDto, User author, Event event);

    @Mapping(target = "eventId", source = "event.id")
    CommentDto toDto(Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isEdited", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateCommentFromDto(UpdateCommentDto dto, @MappingTarget Comment comment);
}