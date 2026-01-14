package ewm.compilation.controller;

import ewm.compilation.dto.CompilationResponse;
import ewm.compilation.dto.NewCompilationRequest;
import ewm.compilation.dto.UpdateCompilationRequest;
import ewm.compilation.service.CompilationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {

    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationResponse createCompilation(@Valid @RequestBody NewCompilationRequest request) {
        return compilationService.createCompilation(request);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long compId) {
        compilationService.deleteCompilation(compId);
    }

    @PatchMapping("/{compId}")
    public CompilationResponse updateCompilation(@PathVariable Long compId,
                                                 @Valid @RequestBody UpdateCompilationRequest request) {
        return compilationService.updateCompilation(compId, request);
    }
}
