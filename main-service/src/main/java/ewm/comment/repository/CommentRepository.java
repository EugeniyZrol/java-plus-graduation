package ewm.comment.repository;

import ewm.comment.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.event " +
            "WHERE c.author.id = :authorId AND c.isDeleted = false " +
            "ORDER BY c.createdDate DESC")
    Page<Comment> findByAuthorIdAndIsDeletedFalse(@Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.event " +
            "WHERE c.id = :id AND c.author.id = :authorId")
    Optional<Comment> findByIdAndAuthorId(@Param("id") Long id, @Param("authorId") Long authorId);

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.event " +
            "WHERE c.event.id IN :eventIds AND c.isDeleted = false")
    List<Comment> findByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.event " +
            "WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findByIdNotDeleted(@Param("id") Long id);

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.event " +
            "WHERE c.event.id = :eventId AND c.isDeleted = false " +
            "ORDER BY c.createdDate DESC")
    Page<Comment> findByEventIdNotDeleted(@Param("eventId") Long eventId, Pageable pageable);
}