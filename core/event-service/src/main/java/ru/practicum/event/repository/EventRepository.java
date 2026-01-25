package ru.practicum.event.repository;

import ru.practicum.event.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllById(@NonNull Iterable<Long> ids);

    boolean existsById(@NonNull Long id);

    Page<Event> findAllByInitiatorIdOrderByCreatedAtDesc(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    boolean existsByCategoryId(Long categoryId);

    Page<Event> findAll(@NonNull Specification<Event> spec, @NonNull Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.id IN :eventIds")
    long countByIdIn(@Param("eventIds") Set<Long> eventIds);

    Event findFirstByOrderByCreatedAtAsc();
}