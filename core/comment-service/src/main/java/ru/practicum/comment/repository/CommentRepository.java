package ru.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "WHERE c.authorId = :authorId AND c.isDeleted = false " +
            "ORDER BY c.createdDate DESC")
    Page<Comment> findByAuthorIdAndIsDeletedFalse(@Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findByIdAndIsDeletedFalse(@Param("id") Long id);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.eventId = :eventId AND c.isDeleted = false " +
            "ORDER BY c.createdDate DESC")
    Page<Comment> findByEventIdAndIsDeletedFalse(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.eventId = :eventId AND c.isDeleted = false " +
            "ORDER BY c.createdDate DESC")
    List<Comment> findByEventIdAndIsDeletedFalse(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.eventId = :eventId AND c.isDeleted = false")
    Integer countByEventIdAndIsDeletedFalse(@Param("eventId") Long eventId);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.authorId = :authorId AND c.isDeleted = false " +
            "ORDER BY c.createdDate DESC")
    List<Comment> findByAuthorIdAndIsDeletedFalse(@Param("authorId") Long authorId);

    @Query("SELECT COUNT(c) > 0 FROM Comment c " +
            "WHERE c.id = :id AND c.isDeleted = false")
    Boolean existsByIdAndIsDeletedFalse(@Param("id") Long id);

    @Query("SELECT COUNT(c) > 0 FROM Comment c " +
            "WHERE c.id = :id AND c.authorId = :authorId AND c.isDeleted = false")
    Boolean existsByIdAndAuthorIdAndIsDeletedFalse(@Param("id") Long id, @Param("authorId") Long authorId);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.id = :id AND c.authorId = :authorId AND c.isDeleted = false")
    Optional<Comment> findByIdAndAuthorIdAndIsDeletedFalseForUpdate(@Param("id") Long id, @Param("authorId") Long authorId);
}