package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.UserInteractionEntity;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteractionEntity, Long> {

    Optional<UserInteractionEntity> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT e.eventId FROM UserInteractionEntity e WHERE e.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT e FROM UserInteractionEntity e " +
            "WHERE e.userId = :userId " +
            "ORDER BY e.lastActionAt DESC")
    List<UserInteractionEntity> findRecentInteractions(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT SUM(e.maxWeight) FROM UserInteractionEntity e WHERE e.eventId = :eventId")
    Double sumWeightsByEventId(@Param("eventId") Long eventId);
}