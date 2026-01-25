package ru.practicum.categories.controller;

import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.categories.service.CategoryService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getCategories(@RequestParam(defaultValue = "0") int from,
                                           @RequestParam(defaultValue = "10") int size) {
        return categoryService.getAllCategories(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryDto getCategory(@PathVariable Long catId) {
        return categoryService.getCategoryById(catId);
    }
}
