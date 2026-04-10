package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Arende;
import se.exempel.sds.dto.ArendeResponse;
import se.exempel.sds.dto.CreateArendeRequest;
import se.exempel.sds.repository.ArendeRepository;
import se.exempel.sds.security.OrganisationPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArendeServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();
    private final AuditService auditService = new AuditService();
    private final ArendeRepository arendeRepository = mock(ArendeRepository.class);

    private final ArendeService arendeService = new ArendeService(arendeRepository, authorizationService, auditService);

    // ── createArende ────────────────────────────────────────────────────────────

    @Test
    void createArende_shouldSetOrganisationFromPrincipal_andStatusOpen() {
        when(arendeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");
        CreateArendeRequest request = new CreateArendeRequest("New arende", null);

        ArendeResponse response = arendeService.createArende(request, principal);

        assertThat(response.status()).isEqualTo(ArendeStatus.OPEN);
        assertThat(response.organisationId()).isEqualTo("organisation-A");
    }

    @Test
    void createArende_shouldUseCreatedByFromPrincipal() {
        when(arendeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("user-42", "organisation-A", "USER");
        CreateArendeRequest request = new CreateArendeRequest("New arende", null);

        ArendeResponse response = arendeService.createArende(request, principal);

        assertThat(response.createdBy()).isEqualTo("user-42");
    }

    // ── getArende ──────────────────────────────────────────────────────────────

    @Test
    void getArende_shouldReturnArende_whenFound() {
        UUID id = UUID.randomUUID();
        Arende existingArende = openArende(id);
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(existingArende));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        ArendeResponse response = arendeService.getArende(id, principal);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(ArendeStatus.OPEN);
    }

    @Test
    void getArende_shouldReturn404_whenArendeBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("user1", "organisation-B", "USER");

        assertThatThrownBy(() -> arendeService.getArende(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── listArenden ─────────────────────────────────────────────────────────────

    @Test
    void listArenden_shouldReturnAllArendenForOrganisation() {
        when(arendeRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(openArende(UUID.randomUUID()), openArende(UUID.randomUUID())));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        List<ArendeResponse> result = arendeService.listArenden(principal, null);

        assertThat(result).hasSize(2)
                .allMatch(c -> "organisation-A".equals(c.organisationId()));
    }

    @Test
    void listArenden_shouldReturnEmpty_whenOrganisationHasNoArenden() {
        when(arendeRepository.findAllByOrganisationId("organisation-A")).thenReturn(List.of());

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThat(arendeService.listArenden(principal, null)).isEmpty();
    }

    // ── closeArende ────────────────────────────────────────────────────────────

    @Test
    void closeArende_shouldDenyAccess_forUserRole() {
        UUID id = UUID.randomUUID();
        Arende openArende = openArende(id);
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(openArende));

        OrganisationPrincipal principal = principal("user1", "organisation-A", "USER");

        assertThatThrownBy(() -> arendeService.closeArende(id, principal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void closeArende_shouldDenyAccess_whenNoRoles() {
        UUID id = UUID.randomUUID();
        Arende openArende = openArende(id);
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(openArende));

        OrganisationPrincipal principal = principal("user1", "organisation-A" /* no roles */);

        assertThatThrownBy(() -> arendeService.closeArende(id, principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void closeArende_shouldSucceed_forAdminRole() {
        UUID id = UUID.randomUUID();
        Arende openArende = openArende(id);
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(openArende));
        when(arendeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        ArendeResponse response = arendeService.closeArende(id, principal);

        assertThat(response.status()).isEqualTo(ArendeStatus.CLOSED);
    }

    @Test
    void closeArende_shouldThrow_whenArendeAlreadyClosed() {
        UUID id = UUID.randomUUID();
        Arende closedArende = Arende.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Arende")
                .status(ArendeStatus.CLOSED)
                .createdBy("test")
                .build();
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(closedArende));

        OrganisationPrincipal principal = principal("admin1", "organisation-A", "ADMIN");

        assertThatThrownBy(() -> arendeService.closeArende(id, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already CLOSED");
    }

    @Test
    void closeArende_shouldReturn404_whenArendeBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("admin1", "organisation-B", "ADMIN");

        assertThatThrownBy(() -> arendeService.closeArende(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void closeArende_shouldDenyAccess_forAdminFromDifferentOrganisation() {
        // An ADMIN in org-B must not reach close-logic for org-A arenden.
        // The repository naturally returns empty for a cross-org lookup → 404.
        UUID id = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        OrganisationPrincipal principal = principal("admin-evil", "organisation-B", "ADMIN");

        assertThatThrownBy(() -> arendeService.closeArende(id, principal))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static OrganisationPrincipal principal(String userId, String organisationId, String... roles) {
        var authorities = Stream.of(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        return new OrganisationPrincipal(userId, organisationId, authorities);
    }

    private static Arende openArende(UUID id) {
        return Arende.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test arende")
                .status(ArendeStatus.OPEN)
                .createdBy("test-user")
                .build();
    }
}
