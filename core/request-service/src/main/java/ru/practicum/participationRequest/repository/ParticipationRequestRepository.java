package ru.practicum.participationRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.participationRequest.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.eventId = :eventId AND pr.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT pr.eventId as eventId, COUNT(pr) as count " +
            "FROM ParticipationRequest pr " +
            "WHERE pr.eventId IN :eventIds AND pr.status = 'CONFIRMED' " +
            "GROUP BY pr.eventId")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "WHERE pr.id IN :requestIds AND pr.eventId = :eventId")
    List<ParticipationRequest> findAllByIdInAndEventId(@Param("requestIds") List<Long> requestIds,
                                                       @Param("eventId") Long eventId);

    boolean existsByEventIdAndRequesterIdAndStatus(Long eventId, Long userId, String status);
}