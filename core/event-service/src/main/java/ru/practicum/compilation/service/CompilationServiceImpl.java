package ru.practicum.compilation.service;

import ru.practicum.interaction.dto.compilation.CompilationResponse;
import ru.practicum.interaction.dto.compilation.NewCompilationRequest;
import ru.practicum.interaction.dto.compilation.UpdateCompilationRequest;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.service.EventService;
import ru.practicum.event.model.Event;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.categories.service.CategoryService;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventService eventService;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final UserClient userClient;
    private final CategoryService categoryService;

    @Override
    @Transactional
    public CompilationResponse createCompilation(NewCompilationRequest request) {
        log.debug("Создание новой компиляции: title={}", request.getTitle());
        Compilation compilation = compilationMapper.toEntity(request);

        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            boolean allEventsExist = eventService.existEventsByIds(request.getEvents());
            if (!allEventsExist) throw new NotFoundException("Некоторые события не найдены");

            Set<Event> events = eventService.getEventsByIds(request.getEvents());
            compilation.setEvents(events);
        } else {
            compilation.setEvents(new HashSet<>());
        }

        try {
            Compilation savedCompilation = compilationRepository.save(compilation);
            log.info("Создана компиляция: ID={}, title={}, событий={}",
                    savedCompilation.getId(), savedCompilation.getTitle(), savedCompilation.getEvents().size());

            Set<EventShortDto> eventDtos = convertEventsToDto(savedCompilation.getEvents());
            return compilationMapper.toDto(savedCompilation, eventDtos);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Компиляция с названием уже существует: " + request.getTitle());
        }
    }

    @Override
    @Transactional
    public CompilationResponse updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.debug("Обновление компиляции: ID={}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Компиляция с идентификатором не найдена: " + compId));

        if (request.getTitle() != null) compilation.setTitle(request.getTitle());
        if (request.getPinned() != null) compilation.setPinned(request.getPinned());

        if (request.getEvents() != null) {
            if (request.getEvents().isEmpty()) {
                compilation.setEvents(new HashSet<>());
            } else {
                boolean allEventsExist = eventService.existEventsByIds(request.getEvents());
                if (!allEventsExist) throw new NotFoundException("Некоторые события не найдены");
                Set<Event> events = eventService.getEventsByIds(request.getEvents());
                compilation.setEvents(events);
            }
        }

        try {
            Compilation updatedCompilation = compilationRepository.save(compilation);
            log.info("Компиляция обновлена: ID={}, title={}, событий={}",
                    compId, updatedCompilation.getTitle(), updatedCompilation.getEvents().size());

            Set<EventShortDto> eventDtos = convertEventsToDto(updatedCompilation.getEvents());
            return compilationMapper.toDto(updatedCompilation, eventDtos);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Компиляция с названием уже существует: " + request.getTitle());
        }
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
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

        Page<Compilation> compilationsPage = (pinned != null)
                ? compilationRepository.findByPinned(pinned, sortedPageable)
                : compilationRepository.findAll(sortedPageable);

        return compilationsPage.getContent().stream()
                .map(compilation -> {
                    Set<EventShortDto> eventDtos = convertEventsToDto(compilation.getEvents());
                    return compilationMapper.toDto(compilation, eventDtos);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompilationResponse getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Компиляция с идентификатором не найдена: " + compId));

        Set<EventShortDto> eventDtos = convertEventsToDto(compilation.getEvents());
        return compilationMapper.toDto(compilation, eventDtos);
    }

    private Set<EventShortDto> convertEventsToDto(Set<Event> events) {
        if (events == null || events.isEmpty()) return new HashSet<>();

        Map<Long, CategoryDto> categories = categoryService.getCategoriesByIds(
                events.stream().map(Event::getCategoryId).collect(Collectors.toSet())
        );
        Map<Long, UserShortDto> users = userClient.getUsersShortByIds(
                events.stream().map(Event::getInitiatorId).collect(Collectors.toSet())
        );

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        categories.get(event.getCategoryId()),
                        users.get(event.getInitiatorId())
                ))
                .collect(Collectors.toSet());
    }
}