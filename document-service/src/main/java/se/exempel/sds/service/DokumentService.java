package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Dokument;
import se.exempel.sds.domain.Arende;
import se.exempel.sds.dto.CreateDokumentRequest;
import se.exempel.sds.dto.DokumentResponse;
import se.exempel.sds.dto.UpdateDokumentRequest;
import se.exempel.sds.repository.ArendeRepository;
import se.exempel.sds.repository.DokumentRepository;
import se.exempel.sds.security.OrganisationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class DokumentService {

    private final DokumentRepository dokumentRepository;
    private final ArendeRepository arendeRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public DokumentService(DokumentRepository dokumentRepository,
                           ArendeRepository arendeRepository,
                           AuthorizationService authorizationService,
                           AuditService auditService) {
        this.dokumentRepository = dokumentRepository;
        this.arendeRepository = arendeRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DokumentResponse getDokument(UUID id, OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertReadAccess(doc, principal);
        auditService.log(AuditAction.DOKUMENT_READ, principal, id);
        return DokumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public List<DokumentResponse> listDokuments(OrganisationPrincipal principal) {
        return dokumentRepository.findAllByOrganisationId(principal.organisationId()).stream()
                .filter(doc -> authorizationService.isReadPermitted(doc, principal))
                .map(DokumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DokumentResponse> listDokumentsByArende(UUID arendeId, OrganisationPrincipal principal) {
        Arende arende = requireArende(arendeId, principal.organisationId());
        authorizationService.assertReadAccess(arende, principal);

        return dokumentRepository.findAllByLinkedArende_IdAndOrganisationId(arendeId, principal.organisationId())
                .stream()
                .filter(doc -> authorizationService.isReadPermitted(doc, principal))
                .map(DokumentResponse::from)
                .toList();
    }

    public DokumentResponse createDokument(CreateDokumentRequest request, OrganisationPrincipal principal) {
        Arende linkedArende = null;

        if (request.arendeId() != null) {
            linkedArende = requireArende(request.arendeId(), principal.organisationId());
            authorizationService.assertSameOrganisationForLink(
                    Dokument.builder().organisationId(principal.organisationId()).build(), linkedArende);

            if (linkedArende.getStatus() == ArendeStatus.CLOSED) {
                throw new IllegalStateException("Cannot add a dokument to a CLOSED arende");
            }
        }

        Dokument doc = Dokument.builder()
                .organisationId(principal.organisationId())
                .title(request.title())
                .description(request.description())
                .classification(request.classification())
                .linkedArende(linkedArende)
                .createdBy(principal.userId())
                .build();

        DokumentResponse response = DokumentResponse.from(dokumentRepository.save(doc));
        auditService.log(AuditAction.DOKUMENT_CREATE, principal, response.id());
        return response;
    }

    public DokumentResponse updateDokument(UUID id, UpdateDokumentRequest request, OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);

        doc.setTitle(request.title());
        doc.setDescription(request.description());
        doc.setClassification(request.classification());
        doc.setUpdatedBy(principal.userId());

        DokumentResponse response = DokumentResponse.from(dokumentRepository.save(doc));
        auditService.log(AuditAction.DOKUMENT_UPDATE, principal, id);
        return response;
    }

    public void deleteDokument(UUID id, OrganisationPrincipal principal) {
        Dokument doc = requireDokument(id, principal.organisationId());
        authorizationService.assertWriteAccess(doc, principal);
        dokumentRepository.delete(doc);
        auditService.log(AuditAction.DOKUMENT_DELETE, principal, id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Dokument requireDokument(UUID id, String organisationId) {
        return dokumentRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException("Dokument not found: " + id));
    }

    private Arende requireArende(UUID id, String organisationId) {
        return arendeRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException("Arende not found: " + id));
    }
}
