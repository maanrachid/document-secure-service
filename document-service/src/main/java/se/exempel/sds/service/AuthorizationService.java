package se.exempel.sds.service;

import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.Arende;
import se.exempel.sds.security.OrganisationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    // Dokument

    public void assertReadAccess(Dokument doc, OrganisationPrincipal principal) {
        assertSameOrganisation(doc.getOrganisationId(), principal);
        assertClassificationAccess(doc.getClassification(), principal);
    }

    public boolean isReadPermitted(Dokument doc, OrganisationPrincipal principal) {
        if (!doc.getOrganisationId().equals(principal.organisationId())) return false;
        return doc.getClassification() != Classification.SECRET
                || principal.hasRole("SUPER_ADMIN");
    }

    public void assertWriteAccess(Dokument doc, OrganisationPrincipal principal) {
        assertSameOrganisation(doc.getOrganisationId(), principal);
        assertClassificationAccess(doc.getClassification(), principal);
        assertArendeOpen(doc);
    }

    // Arende

    public void assertReadAccess(Arende dokumentArende, OrganisationPrincipal principal) {
        assertSameOrganisation(dokumentArende.getOrganisationId(), principal);
    }

    public void assertCloseAccess(Arende dokumentArende, OrganisationPrincipal principal) {
        assertSameOrganisation(dokumentArende.getOrganisationId(), principal);
        assertRole(principal, "ADMIN", "Only ADMIN may close a arende");
    }

    // Dokument+Arende

    public void assertSameOrganisationForLink(Dokument doc, Arende dokumentArende) {
        if (!doc.getOrganisationId().equals(dokumentArende.getOrganisationId())) {
            throw new AccessDeniedException(
                    "Dokument and arende must belong to the same organisation");
        }
    }

    // Private helpers

    private void assertSameOrganisation(String resourceOrganisationId, OrganisationPrincipal principal) {
        if (!resourceOrganisationId.equals(principal.organisationId())) {
            throw new AccessDeniedException("Cross-organisation access denied");
        }
    }

    private void assertClassificationAccess(Classification classification,
                                            OrganisationPrincipal principal) {
        if (classification == Classification.SECRET) {
            assertRole(principal, "SUPER_ADMIN", "SECRET dokuments require SUPER_ADMIN clearance");
        }
    }

    private void assertRole(OrganisationPrincipal principal, String role, String message) {
        if (!principal.hasRole(role)) {
            throw new AccessDeniedException(message);
        }
    }

    private void assertArendeOpen(Dokument doc) {
        if (doc.getLinkedArende() != null
                && doc.getLinkedArende().getStatus() == ArendeStatus.CLOSED) {
            throw new IllegalStateException(
                    "Cannot modify a dokument that is linked to a CLOSED arende");
        }
    }
}
