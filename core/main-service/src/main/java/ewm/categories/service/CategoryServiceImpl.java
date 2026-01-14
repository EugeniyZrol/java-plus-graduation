package ewm.categories.service;

import ewm.categories.dto.CategoryDto;
import ewm.categories.dto.NewCategoryDto;
import ewm.categories.mapper.CategoryMapper;
import ewm.categories.model.Category;
import ewm.categories.repository.CategoryRepository;
import ewm.event.repository.EventRepository;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
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
            log.warn("Попытка удаления несуществующей категории: ID={}", catId);
            throw new NotFoundException("Категория с ID=" + catId + " не найдена.");
        }

        if (eventRepository.existsByCategoryId(catId)) {
            log.warn("Попытка удаления категории с привязанными событиями: ID={}", catId);
            throw new ConflictException("Нельзя удалить категорию, с которой связаны события.");
        }

        categoryRepository.deleteById(catId);
        log.info("Категория удалена: ID={}", catId);
    }

    @Transactional
    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto categoryDto) {
        Category categoryToUpdate = categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.warn("Попытка обновления несуществующей категории: ID={}", catId);
                    return new NotFoundException("Категория с ID=" + catId + " не найдена.");
                });

        String newName = categoryDto.getName();

        if (newName.equals(categoryToUpdate.getName())) {
            log.debug("Имя категории не изменилось: ID={}, name={}", catId, newName);
            return categoryMapper.toCategoryDto(categoryToUpdate);
        }

        if (categoryRepository.existsByName(newName)) {
            log.warn("Попытка обновления категории на уже существующее имя: ID={}, newName={}", catId, newName);
            throw new ConflictException("Имя категории '" + newName + "' уже занято.");
        }

        categoryToUpdate.setName(newName);
        Category savedCategory = categoryRepository.save(categoryToUpdate);
        log.info("Категория обновлена: ID={}, новое имя={}", catId, newName);

        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public List<CategoryDto> getAllCategories(int from, int size) {
        System.out.println("getAllCategories called with from=" + from + ", size=" + size);

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
        log.debug("Поиск категории по ID: {}", catId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.warn("Категория не найдена: ID={}", catId);
                    return new NotFoundException("Категория с ID=" + catId + " не найдена.");
                });

        log.debug("Категория найдена: ID={}, name={}", catId, category.getName());
        return categoryMapper.toCategoryDto(category);
    }
}