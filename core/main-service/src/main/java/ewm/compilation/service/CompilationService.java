package ewm.compilation.service;

import ewm.compilation.dto.CompilationResponse;
import ewm.compilation.dto.NewCompilationRequest;
import ewm.compilation.dto.UpdateCompilationRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompilationService {
    CompilationResponse createCompilation(NewCompilationRequest request);

    void deleteCompilation(Long compId);

    CompilationResponse updateCompilation(Long compId, UpdateCompilationRequest request);

    List<CompilationResponse> getCompilations(Boolean pinned, Pageable pageable);

    CompilationResponse getCompilationById(Long compId);
}
