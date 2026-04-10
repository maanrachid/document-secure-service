package se.exempel.sds.exception;

import se.exempel.sds.security.OrganisationPrincipal;
import se.exempel.sds.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Translates domain exceptions to HTTP responses.
 *
 * <p>Error messages are intentionally vague to avoid leaking internal details
 * to clients. The full message is available in server-side logs only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied(HttpServletRequest request) {
        auditService.logAccessDenied(extractPrincipal(), request.getRequestURI());
        return Map.of("error", "Access denied");
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound() {
        return Map.of("error", "Resource not found");
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(IllegalStateException ex) {
        // Safe to expose – these messages describe business rule violations, not internals
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        // Safe to expose – these messages describe input validation failures
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return Map.of("error", "Validation failed", "details", details);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Extracts the OrganisationPrincipal from the current SecurityContext.
     * Returns null for unauthenticated requests (bad/missing JWT, etc.).
     */
    private static OrganisationPrincipal extractPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OrganisationPrincipal principal) {
            return principal;
        }
        return null;
    }
}
