package ru.practicum.categories.service;

import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.categories.NewCategoryDto;
import ru.practicum.categories.mapper.CategoryMapper;
import ru.practicum.categories.model.Category;
import ru.practicum.categories.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        String categoryName = newCategoryDto.getName();
        if (categoryRepository.existsByName(categoryName)) {
            throw new ConflictException("Имя категории '" + categoryName + "' уже занято.");
        }

        Category category = categoryMapper.toCategory(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Создана новая категория: ID={}, name={}", savedCategory.getId(), savedCategory.getName());
        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Категория с ID=" + catId + " не найдена.");
        }
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Нельзя удалить категорию, с которой связаны события.");
        }
        categoryRepository.deleteById(catId);
        log.info("Категория удалена: ID={}", catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto categoryDto) {
        Category categoryToUpdate = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID=" + catId + " не найдена."));

        String newName = categoryDto.getName();
        if (newName.equals(categoryToUpdate.getName())) {
            return categoryMapper.toCategoryDto(categoryToUpdate);
        }
        if (categoryRepository.existsByName(newName)) {
            throw new ConflictException("Имя категории '" + newName + "' уже занято.");
        }

        categoryToUpdate.setName(newName);
        Category savedCategory = categoryRepository.save(categoryToUpdate);
        log.info("Категория обновлена: ID={}, новое имя={}", catId, newName);
        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public List<CategoryDto> getAllCategories(int from, int size) {
        int pageNumber = from / size;
        PageRequest page = PageRequest.of(pageNumber, size);
        List<Category> categories = categoryRepository.findAllByOrderByIdDesc(page);
        log.debug("Найдено категорий: {}", categories.size());
        return categories.stream()
                .map(categoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID=" + catId + " не найдена."));
        log.debug("Категория найдена: ID={}, name={}", catId, category.getName());
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    public Map<Long, CategoryDto> getCategoriesByIds(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return Map.of();

        List<Category> categories = categoryRepository.findAllById(categoryIds);
        return categories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        categoryMapper::toCategoryDto
                ));
    }
}