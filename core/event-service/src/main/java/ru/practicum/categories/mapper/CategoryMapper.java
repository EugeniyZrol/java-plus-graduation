package ru.practicum.categories.mapper;

import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.categories.NewCategoryDto;
import ru.practicum.categories.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toCategory(NewCategoryDto newCategoryDto);

    CategoryDto toCategoryDto(Category category);
}