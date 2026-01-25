package ru.practicum.comment.mapper;

import org.mapstruct.*;
import ru.practicum.comment.model.Comment;
import ru.practicum.interaction.dto.comment.CommentDto;
import ru.practicum.interaction.dto.comment.NewCommentDto;
import ru.practicum.interaction.dto.comment.UpdateCommentDto;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isEdited", constant = "false")
    @Mapping(target = "isDeleted", constant = "false")
    Comment toComment(NewCommentDto newCommentDto);

    @Mapping(target = "author", ignore = true) // Будет установлен отдельно
    @Mapping(target = "eventId", source = "eventId")
    CommentDto toDto(Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isEdited", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateCommentFromDto(UpdateCommentDto dto, @MappingTarget Comment comment);
}