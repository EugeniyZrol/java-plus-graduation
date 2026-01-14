package ewm.categories.mapper;

import ewm.categories.dto.CategoryDto;
import ewm.categories.dto.NewCategoryDto;
import ewm.categories.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toCategory(NewCategoryDto newCategoryDto);

    CategoryDto toCategoryDto(Category category);
}