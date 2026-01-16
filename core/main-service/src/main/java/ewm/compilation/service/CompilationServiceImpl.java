package ewm.compilation.service;

import ewm.compilation.dto.CompilationResponse;
import ewm.compilation.dto.NewCompilationRequest;
import ewm.compilation.dto.UpdateCompilationRequest;
import ewm.compilation.mapper.CompilationMapper;
import ewm.compilation.model.Compilation;
import ewm.compilation.repository.CompilationRepository;
import ewm.event.model.Event;
import ewm.event.repository.EventRepository;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationResponse createCompilation(NewCompilationRequest request) {
        log.debug("Создание новой компиляции: title={}", request.getTitle());

        Compilation compilation = compilationMapper.toEntity(request);

        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(new ArrayList<>(request.getEvents()));

            if (events.size() != request.getEvents().size()) {
                log.warn("Не все события найдены при создании компиляции: requested={}, found={}",
                        request.getEvents().size(), events.size());
                throw new NotFoundException("Некоторые события не найдены");
            }
            compilation.setEvents(new HashSet<>(events));
            log.debug("Добавлено событий в компиляцию: {}", events.size());
        } else {
            compilation.setEvents(new HashSet<>());
        }

        try {
            Compilation savedCompilation = compilationRepository.save(compilation);
            log.info("Создана новая компиляция: ID={}, title={}, событий={}",
                    savedCompilation.getId(), savedCompilation.getTitle(), savedCompilation.getEvents().size());

            return compilationMapper.toDto(savedCompilation);
        } catch (DataIntegrityViolationException e) {
            log.warn("Попытка создания компиляции с существующим названием: {}", request.getTitle());
            throw new ConflictException("Компиляция с названием уже существует: " + request.getTitle());
        }
    }

    @Override
    @Transactional
    public CompilationResponse updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.debug("Обновление компиляции: ID={}, title={}, pinned={}",
                compId, request.getTitle(), request.getPinned());

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.warn("Попытка обновления несуществующей компиляции: ID={}", compId);
                    return new NotFoundException("Компиляция с идентификатором не найдена: " + compId);
                });

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
            log.debug("Обновлено название компиляции: {}", request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
            log.debug("Обновлен статус закрепления: {}", request.getPinned());
        }

        if (request.getEvents() != null) {
            if (request.getEvents().isEmpty()) {
                compilation.setEvents(new HashSet<>());
                log.debug("Очищены события компиляции");
            } else {
                List<Event> events = eventRepository.findAllById(new ArrayList<>(request.getEvents()));
                if (events.size() != request.getEvents().size()) {
                    log.warn("Не все события найдены при обновлении компиляции: requested={}, found={}",
                            request.getEvents().size(), events.size());
                    throw new NotFoundException("Некоторые события не найдены");
                }
                compilation.setEvents(new HashSet<>(events));
                log.debug("Обновлены события компиляции: количество={}", events.size());
            }
        }

        try {
            Compilation updatedCompilation = compilationRepository.save(compilation);
            log.info("Компиляция обновлена: ID={}, title={}, событий={}",
                    compId, updatedCompilation.getTitle(), updatedCompilation.getEvents().size());

            return compilationMapper.toDto(updatedCompilation);
        } catch (DataIntegrityViolationException e) {
            log.warn("Попытка обновления на существующее название: {}", request.getTitle());
            throw new ConflictException("Компиляция с названием уже существует: " + request.getTitle());
        }
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.debug("Удаление компиляции: ID={}", compId);

        if (!compilationRepository.existsById(compId)) {
            log.warn("Попытка удаления несуществующей компиляции: ID={}", compId);
            throw new NotFoundException("Компиляция с идентификатором не найдена: " + compId);
        }

        compilationRepository.deleteById(compId);
        log.info("Компиляция удалена: ID={}", compId);
    }

    @Override
    public List<CompilationResponse> getCompilations(Boolean pinned, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("id").ascending()
        );

        Page<Compilation> compilationsPage;

        if (pinned != null) {
            compilationsPage = compilationRepository.findByPinned(pinned, sortedPageable);
        } else {
            compilationsPage = compilationRepository.findAll(sortedPageable);
        }

        return compilationsPage.getContent().stream()
                .map(compilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationResponse getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Компиляция с идентификатором не найдена: " + compId));
        return compilationMapper.toDto(compilation);
    }
}