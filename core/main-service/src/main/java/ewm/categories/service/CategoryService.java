package ewm.categories.service;

import ewm.categories.dto.CategoryDto;
import ewm.categories.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto);

    List<CategoryDto> getAllCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);
}