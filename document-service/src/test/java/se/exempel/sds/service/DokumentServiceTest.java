package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.Arende;
import se.exempel.sds.dto.CreateDokumentRequest;
import se.exempel.sds.dto.DokumentResponse;
import se.exempel.sds.dto.UpdateDokumentRequest;
import se.exempel.sds.repository.ArendeRepository;
import se.exempel.sds.repository.DokumentRepository;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokumentServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();
    private final AuditService auditService = new AuditService();
    private final DokumentRepository dokumentRepository = mock(DokumentRepository.class);
    private final ArendeRepository arendeRepository = mock(ArendeRepository.class);

    private final DokumentService dokumentService = new DokumentService(
            dokumentRepository, arendeRepository, authorizationService, auditService);

    // ── getDokument ──────────────────────────────────────────────────────────

    @Test
    void getDokument_shouldReturn404_whenDokumentBelongsToOtherOrganisation() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-B"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dokumentService.getDokument(id, principal("user1", "organisation-B", "USER")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getDokument_shouldDeny_forSecretDokumentWithoutSuperAdmin() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(dokument(id, Classification.SECRET)));

        assertThatThrownBy(() -> dokumentService.getDokument(id, principal("user1", "organisation-A", "USER")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    @Test
    void getDokument_shouldAllow_forSecretDokumentWithSuperAdmin() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(dokument(id, Classification.SECRET)));

        DokumentResponse response = dokumentService.getDokument(id, principal("sa1", "organisation-A", "SUPER_ADMIN"));
        assertThat(response.classification()).isEqualTo(Classification.SECRET);
    }

    // ── listDokuments ────────────────────────────────────────────────────────

    @Test
    void listDokuments_shouldSilentlyFilterOut_secretDokumentsForNonSuperAdmin() {
        when(dokumentRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(
                        dokument(UUID.randomUUID(), Classification.PUBLIC),
                        dokument(UUID.randomUUID(), Classification.SECRET)));

        List<DokumentResponse> result = dokumentService.listDokuments(principal("user1", "organisation-A", "USER"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().classification()).isEqualTo(Classification.PUBLIC);
    }

    @Test
    void listDokuments_shouldShowAllClassifications_forSuperAdmin() {
        when(dokumentRepository.findAllByOrganisationId("organisation-A"))
                .thenReturn(List.of(
                        dokument(UUID.randomUUID(), Classification.PUBLIC),
                        dokument(UUID.randomUUID(), Classification.SECRET)));

        List<DokumentResponse> result = dokumentService.listDokuments(principal("sa1", "organisation-A", "SUPER_ADMIN"));

        assertThat(result).hasSize(2);
    }

    // ── createDokument ───────────────────────────────────────────────────────

    @Test
    void createDokument_shouldSucceed_whenNoArendeLinked() {
        when(dokumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDokumentRequest request = new CreateDokumentRequest(
                "Title", null, Classification.PUBLIC, null);

        assertThatNoException().isThrownBy(() ->
                dokumentService.createDokument(request, principal("user1", "organisation-A", "USER")));

        verify(arendeRepository, never()).findByIdAndOrganisationId(any(), any());
    }

    @Test
    void createDokument_shouldThrow_whenLinkedArendeIsClosed() {
        UUID arendeId = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.of(closedArende(arendeId)));

        CreateDokumentRequest request = new CreateDokumentRequest(
                "Title", null, Classification.PUBLIC, arendeId);

        assertThatThrownBy(() ->
                dokumentService.createDokument(request, principal("user1", "organisation-A", "USER")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void createDokument_shouldThrow_whenArendeBelongsToOtherOrganisation() {
        UUID arendeId = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.empty());

        CreateDokumentRequest request = new CreateDokumentRequest(
                "Title", null, Classification.PUBLIC, arendeId);

        assertThatThrownBy(() ->
                dokumentService.createDokument(request, principal("user1", "organisation-A", "USER")))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── updateDokument ───────────────────────────────────────────────────────

    @Test
    void updateDokument_shouldReturn404_whenDokumentNotFound() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                dokumentService.updateDokument(id,
                        new UpdateDokumentRequest("New title", null, Classification.PUBLIC),
                        principal("user1", "organisation-A", "USER")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateDokument_shouldDeny_forSecretDokument_withRegularUser() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(dokument(id, Classification.SECRET)));

        assertThatThrownBy(() ->
                dokumentService.updateDokument(id,
                        new UpdateDokumentRequest("New title", null, Classification.SECRET),
                        principal("user1", "organisation-A", "USER")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SECRET");
    }

    // ── deleteDokument ───────────────────────────────────────────────────────

    @Test
    void deleteDokument_shouldReturn404_whenDokumentNotFound() {
        UUID id = UUID.randomUUID();
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dokumentService.deleteDokument(id, principal("user1", "organisation-A", "USER")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteDokument_shouldSucceed_andCallRepositoryDelete() {
        UUID id = UUID.randomUUID();
        Dokument doc = dokument(id, Classification.PUBLIC);
        when(dokumentRepository.findByIdAndOrganisationId(id, "organisation-A"))
                .thenReturn(Optional.of(doc));

        assertThatNoException().isThrownBy(() ->
                dokumentService.deleteDokument(id, principal("user1", "organisation-A", "USER")));

        verify(dokumentRepository).delete(doc);
    }

    // ── listDokumentsByArende ────────────────────────────────────────────────

    @Test
    void listDokumentsByArende_shouldReturn404_whenArendeNotFound() {
        UUID arendeId = UUID.randomUUID();
        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                dokumentService.listDokumentsByArende(arendeId, principal("user1", "organisation-A", "USER")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listDokumentsByArende_shouldFilterSecretDokuments_forNonSuperAdmin() {
        UUID arendeId = UUID.randomUUID();
        Arende openArende = Arende.builder()
                .id(arendeId).organisationId("organisation-A")
                .title("Arende").status(ArendeStatus.OPEN).createdBy("test").build();

        when(arendeRepository.findByIdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(Optional.of(openArende));
        when(dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(arendeId, "organisation-A"))
                .thenReturn(List.of(
                        dokument(UUID.randomUUID(), Classification.INTERNAL),
                        dokument(UUID.randomUUID(), Classification.SECRET)));

        List<DokumentResponse> result = dokumentService.listDokumentsByArende(
                arendeId, principal("user1", "organisation-A", "USER"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().classification()).isEqualTo(Classification.INTERNAL);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static OrganisationPrincipal principal(String userId, String organisationId, String... roles) {
        var authorities = Stream.of(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        return new OrganisationPrincipal(userId, organisationId, authorities);
    }

    private static Dokument dokument(UUID id, Classification classification) {
        return Dokument.builder()
                .id(id)
                .organisationId("organisation-A")
                .title("Test doc")
                .classification(classification)
                .createdBy("test-user")
                .build();
    }

    private static Arende closedArende(UUID id) {
        return Arende.builder()
                .id(id).organisationId("organisation-A")
                .title("Test arende").status(ArendeStatus.CLOSED).createdBy("test-user").build();
    }
}
