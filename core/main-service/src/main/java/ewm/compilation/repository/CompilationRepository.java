package ewm.compilation.repository;

import ewm.compilation.model.Compilation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long>, JpaSpecificationExecutor<Compilation> {

    Page<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    Optional<Compilation> findById(@NonNull Long id);

    boolean existsById(@NonNull Long compId);
}