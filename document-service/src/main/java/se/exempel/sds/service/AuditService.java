package se.exempel.sds.service;

import se.exempel.sds.security.OrganisationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void log(AuditAction action, OrganisationPrincipal principal, UUID resourceId) {
        AUDIT.info("action={} actor={} organisation={} resourceId={} outcome=SUCCESS",
                action.name(),
                sanitize(principal.userId()),
                sanitize(principal.organisationId()),
                resourceId);
    }

    public void logAccessDenied(@Nullable OrganisationPrincipal principal, String resource) {
        String actor  = principal != null ? sanitize(principal.userId())  : "(unauthenticated)";
        String organisation = principal != null ? sanitize(principal.organisationId()) : "(none)";

        AUDIT.warn("action={} actor={} organisation={} resource={} outcome=FAILURE",
                AuditAction.ACCESS_DENIED.name(),
                actor,
                organisation,
                sanitize(resource));
    }

    static String sanitize(String input) {
        if (input == null) {
            return "(null)";
        }
        // \p{Cntrl} covers U+0000–U+001F and U+007F (all ASCII control chars incl. CR/LF/TAB)
        return input.replaceAll("\\p{Cntrl}", "_");
    }
}