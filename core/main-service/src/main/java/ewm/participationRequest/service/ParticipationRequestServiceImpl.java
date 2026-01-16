package ewm.participationRequest.service;
import ewm.event.model.Event;
import ewm.event.repository.EventRepository;
import ewm.event.service.EventStatsService;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.participationRequest.dto.EventRequestStatusUpdateRequest;
import ewm.participationRequest.dto.EventRequestStatusUpdateResult;
import ewm.participationRequest.dto.ParticipationRequestDto;
import ewm.participationRequest.mapper.ParticipationRequestMapper;
import ewm.participationRequest.model.ParticipationRequest;
import ewm.participationRequest.model.RequestStatus;
import ewm.participationRequest.repository.ParticipationRequestRepository;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper requestMapper;
    private final EventStatsService eventStatsService;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        return requestRepository.findAllByRequesterIdWithEventAndRequester(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса для пользователя с id: {} на событие с id: {}", userId, eventId);

        Event event = eventRepository.findByIdWithInitiator(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя участвовать в собственном событии");
        }

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Запрос на участие в этом событии уже существует");
        }

        Map<Long, Long> confirmedRequestsMap = eventStatsService.getConfirmedRequestsBatch(List.of(eventId));
        Long confirmedRequests = confirmedRequestsMap.getOrDefault(eventId, 0L);

        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников для этого события");
        }

        RequestStatus status;
        if (!event.getIsRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        User userProxy = userRepository.getReferenceById(userId);

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(userProxy)
                .event(event)
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

        Event event = eventRepository.findByIdWithInitiator(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
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

        List<ParticipationRequest> requestsToUpdate =
                requestRepository.findAllByIdWithEventAndRequester(request.getRequestIds());

        if (requestsToUpdate.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        Map<Long, Long> confirmedRequestsMap = eventStatsService.getConfirmedRequestsBatch(List.of(eventId));
        Long currentConfirmedCount = confirmedRequestsMap.getOrDefault(eventId, 0L);

        int participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;

        for (ParticipationRequest participationRequest : requestsToUpdate) {
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно изменять только запросы в статусе PENDING. " +
                        "Запрос с id = " + participationRequest.getId() + " имеет статус: " +
                        participationRequest.getStatus());
            }

            if (!participationRequest.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Запрос с id = " + participationRequest.getId() +
                        " не принадлежит событию с id = " + eventId);
            }

            if (newStatus == RequestStatus.CONFIRMED) {
                if (participantLimit > 0 && currentConfirmedCount >= participantLimit) {
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
        ParticipationRequest request = requestRepository.findByIdWithEventAndRequester(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdWithInitiator(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return requestRepository.findAllByEventIdWithEventAndRequester(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }
}
