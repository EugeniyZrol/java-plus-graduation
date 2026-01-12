package ewm.participationRequest.repository;

import ewm.participationRequest.dto.EventConfirmedRequestsDto;
import ewm.participationRequest.model.ParticipationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    @Query("SELECT NEW ewm.participationRequest.dto.EventConfirmedRequestsDto(" +
            "pr.event.id, COUNT(pr)) " +
            "FROM ParticipationRequest pr " +
            "WHERE pr.event.id IN :eventIds AND pr.status = ewm.participationRequest.model.RequestStatus.CONFIRMED " +
            "GROUP BY pr.event.id")
    List<EventConfirmedRequestsDto> findConfirmedRequestsCountByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.requester.id = :userId")
    List<ParticipationRequest> findAllByRequesterIdWithEventAndRequester(@Param("userId") Long userId);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.event.id = :eventId")
    List<ParticipationRequest> findAllByEventIdWithEventAndRequester(@Param("eventId") Long eventId);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.id IN :requestIds")
    List<ParticipationRequest> findAllByIdWithEventAndRequester(@Param("requestIds") List<Long> requestIds);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.id = :requestId AND pr.requester.id = :userId")
    Optional<ParticipationRequest> findByIdWithEventAndRequester(@Param("requestId") Long requestId,
                                                                 @Param("userId") Long userId);
}