package ru.practicum.categories.controller;

import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.categories.NewCategoryDto;
import ru.practicum.categories.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoryService.createCategory(newCategoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategory(catId);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(@PathVariable Long catId,
                                      @Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoryService.updateCategory(catId, newCategoryDto);
    }
}
