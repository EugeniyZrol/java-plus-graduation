package ru.practicum.participationRequest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.interaction.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.request.ParticipationRequestDto;
import ru.practicum.interaction.enums.request.RequestStatus;
import ru.practicum.interaction.client.feign.EventClient;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.participationRequest.mapper.ParticipationRequestMapper;
import ru.practicum.participationRequest.model.ParticipationRequest;
import ru.practicum.participationRequest.repository.ParticipationRequestRepository;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestMapper requestMapper;
    private final RequestStatsService requestStatsService;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        Boolean userExists = userClient.existsUserById(userId);
        if (Boolean.FALSE.equals(userExists)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса для пользователя с id: {} на событие с id: {}", userId, eventId);

        Boolean userExists = userClient.existsUserById(userId);
        if (Boolean.FALSE.equals(userExists)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Boolean eventExists = eventClient.existsEventById(eventId);
        if (Boolean.FALSE.equals(eventExists)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        Long initiatorId = eventClient.getEventInitiatorId(eventId);
        if (initiatorId != null && initiatorId.equals(userId)) {
            throw new ConflictException("Нельзя участвовать в собственном событии");
        }

        String eventState = eventClient.getEventState(eventId);
        if (!"PUBLISHED".equals(eventState)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Запрос на участие в этом событии уже существует");
        }

        Long confirmedRequests = requestStatsService.getConfirmedRequestsCount(eventId);
        Integer participantLimit = eventClient.getParticipantLimit(eventId);

        if (participantLimit != null && participantLimit > 0 && confirmedRequests >= participantLimit) {
            throw new ConflictException("Достигнут лимит участников для этого события");
        }

        RequestStatus status;
        Boolean requiresModeration = eventClient.isRequestModerationEnabled(eventId);
        if (Boolean.FALSE.equals(requiresModeration) || (participantLimit != null && participantLimit == 0)) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Запрос создан с id: {}", savedRequest.getId());

        return requestMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        log.info("Изменение статуса запросов для события с id: {} от пользователя с id: {}", eventId, userId);

        Long initiatorId = eventClient.getEventInitiatorId(eventId);
        if (initiatorId == null || !initiatorId.equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        Boolean requiresModeration = eventClient.isRequestModerationEnabled(eventId);
        Integer participantLimit = eventClient.getParticipantLimit(eventId);

        if (Boolean.FALSE.equals(requiresModeration) || (participantLimit != null && participantLimit == 0)) {
            throw new ConflictException("Для этого события не требуется модерация заявок");
        }

        RequestStatus newStatus;
        try {
            newStatus = RequestStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new ConflictException("Недопустимый статус: " + request.getStatus());
        }

        if (newStatus != RequestStatus.CONFIRMED && newStatus != RequestStatus.REJECTED) {
            throw new ConflictException("Можно установить только статусы CONFIRMED или REJECTED");
        }

        List<ParticipationRequest> requestsToUpdate = requestRepository.findAllByIdInAndEventId(
                request.getRequestIds(), eventId
        );

        if (requestsToUpdate.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        Long currentConfirmedCount = requestStatsService.getConfirmedRequestsCount(eventId);

        for (ParticipationRequest participationRequest : requestsToUpdate) {
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно изменять только запросы в статусе PENDING. " +
                        "Запрос с id = " + participationRequest.getId() + " имеет статус: " +
                        participationRequest.getStatus());
            }

            if (newStatus == RequestStatus.CONFIRMED) {
                if (participantLimit != null && participantLimit > 0 &&
                        currentConfirmedCount >= participantLimit) {
                    throw new ConflictException("Достигнут лимит участников для события");
                }
                participationRequest.setStatus(RequestStatus.CONFIRMED);
                currentConfirmedCount++;
                confirmedRequests.add(requestMapper.toDto(participationRequest));
            } else {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.toDto(participationRequest));
            }
        }

        requestRepository.saveAll(requestsToUpdate);

        log.info("Обновлено статусов: подтверждено - {}, отклонено - {}",
                confirmedRequests.size(), rejectedRequests.size());

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId) {
        Long initiatorId = eventClient.getEventInitiatorId(eventId);
        if (initiatorId == null || !initiatorId.equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public int getConfirmedRequestsCountForFeign(Long eventId) {
        Long count = requestStatsService.getConfirmedRequestsCount(eventId);
        return count != null ? count.intValue() : 0;
    }
}