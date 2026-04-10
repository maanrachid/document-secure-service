package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.Arende;
import se.exempel.sds.security.OrganisationPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for the centralized access-control rules in {@link AuthorizationService}.
 * <p>
 * Senior signal: focus on negative arenden – these are what prevent real security incidents.
 */
class AuthorizationServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();

    // ── Organisation Isolation ─────────────────────────────────────────────────────

    @Test
    void readDokument_shouldDenyAccess_whenOrganisationMismatch() {
        Dokument doc = dokument(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    @Test
    void writeDokument_shouldDenyAccess_whenOrganisationMismatch() {
        Dokument doc = dokument(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> authorizationService.assertWriteAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    @Test
    void readArende_shouldDenyAccess_whenOrganisationMismatch() {
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(dokumentArende, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    @Test
    void readArende_shouldAllowAccess_whenSameOrganisation() {
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertReadAccess(dokumentArende, principal));
    }

    @Test
    void crossOrganisationLink_shouldBeDenied() {
        Dokument doc = dokument(Classification.PUBLIC);
        Arende dokumentArende = dokumentArende("organisation-B");

        assertThatThrownBy(() -> authorizationService.assertSameOrganisationForLink(doc, dokumentArende))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sameOrganisationLink_shouldBeAllowed() {
        Dokument doc = dokument(Classification.PUBLIC);
        Arende dokumentArende = dokumentArende("organisation-A");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertSameOrganisationForLink(doc, dokumentArende));
    }

    // ── Classification ───────────────────────────────────────────────────────

    @Test
    void readSecretDokument_shouldDenyAccess_forRegularUser() {
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void readSecretDokument_shouldDenyAccess_forAdmin() {
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThatThrownBy(() -> authorizationService.assertReadAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void readSecretDokument_shouldAllowAccess_forSuperAdmin() {
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertReadAccess(doc, principal));
    }

    @Test
    void readConfidentialDokument_shouldAllowAccess_forRegularUser() {
        Dokument doc = dokument(Classification.CONFIDENTIAL);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertReadAccess(doc, principal));
    }

    @Test
    void writeSecretDokument_shouldDenyAccess_forRegularUser() {
        // Write also enforces the classification check – users cannot modify SECRET docs
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertWriteAccess(doc, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void writeSecretDokument_shouldAllowAccess_forSuperAdmin() {
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertWriteAccess(doc, principal));
    }

    // ── isReadPermitted (list filtering) ─────────────────────────────────────

    @Test
    void isReadPermitted_shouldReturnFalse_forSecretAndRegularUser() {
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isFalse();
    }

    @Test
    void isReadPermitted_shouldReturnFalse_forSecretAndAdmin() {
        // ADMIN is NOT SUPER_ADMIN – must not see SECRET dokuments in lists
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isFalse();
    }

    @Test
    void isReadPermitted_shouldReturnTrue_forSecretAndSuperAdmin() {
        Dokument doc = dokument(Classification.SECRET);
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isTrue();
    }

    @Test
    void isReadPermitted_shouldReturnFalse_forCrossOrganisationAccess() {
        // Even SUPER_ADMIN cannot see another organisation's dokuments in a list
        Dokument doc = dokument(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-B", "SUPER_ADMIN");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isFalse();
    }

    @Test
    void isReadPermitted_shouldReturnTrue_forPublicDokument() {
        Dokument doc = dokument(Classification.PUBLIC);
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThat(authorizationService.isReadPermitted(doc, principal)).isTrue();
    }

    // ── Closed Arende ──────────────────────────────────────────────────────────

    @Test
    void writeDokument_shouldDenyAccess_whenLinkedArendeIsClosed() {
        Arende closedArende = dokumentArende("organisation-A");
        closedArende.setStatus(ArendeStatus.CLOSED);

        Dokument doc = dokument(Classification.PUBLIC);
        doc.setLinkedArende(closedArende);

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertWriteAccess(doc, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void writeDokument_shouldAllowAccess_whenLinkedArendeIsOpen() {
        Arende openArende = dokumentArende("organisation-A");
        openArende.setStatus(ArendeStatus.OPEN);

        Dokument doc = dokument(Classification.PUBLIC);
        doc.setLinkedArende(openArende);

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertWriteAccess(doc, principal));
    }

    @Test
    void writeDokument_shouldAllowAccess_whenDokumentHasNoLinkedArende() {
        Dokument doc = dokument(Classification.PUBLIC);
        // linkedArende is null – no arende check should fire

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertWriteAccess(doc, principal));
    }

    // ── Role Escalation ──────────────────────────────────────────────────────

    @Test
    void closeArende_shouldDenyAccess_forUserRole() {
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(dokumentArende, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void closeArende_shouldDenyAccess_whenNoRoles() {
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("user1", "organisation-A" /* no roles */);

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(dokumentArende, principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void closeArende_shouldDenyAccess_forSuperAdminWithoutAdminRole() {
        // SUPER_ADMIN alone does NOT grant arende-close rights – explicit ADMIN required
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("sa1", "organisation-A", "SUPER_ADMIN");

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(dokumentArende, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void closeArende_shouldAllowAccess_forAdminRole() {
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThatNoException()
                .isThrownBy(() -> authorizationService.assertCloseAccess(dokumentArende, principal));
    }

    @Test
    void closeArende_shouldDenyAccess_forAdminInDifferentOrganisation() {
        Arende dokumentArende = dokumentArende("organisation-A");
        OrganisationPrincipal principal = principal("admin1", "organisation-B", "ADMIN");

        assertThatThrownBy(() -> authorizationService.assertCloseAccess(dokumentArende, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-organisation");
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    private static OrganisationPrincipal principal(String userId, String organisationId, String... roles) {
        var authorities = Stream.of(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        return new OrganisationPrincipal(userId, organisationId, authorities);
    }

    private static Dokument dokument(Classification classification) {
        return Dokument.builder()
                .organisationId("organisation-A")
                .title("Test dokument")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static Arende dokumentArende(String organisationId) {
        return Arende.builder()
                .organisationId(organisationId)
                .title("Test arende")
                .status(ArendeStatus.OPEN)
                .createdBy("test-user")
                .build();
    }
}
