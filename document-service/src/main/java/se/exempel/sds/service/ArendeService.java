package se.exempel.sds.service;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Arende;
import se.exempel.sds.dto.ArendeResponse;
import se.exempel.sds.dto.CreateArendeRequest;
import se.exempel.sds.repository.ArendeRepository;
import se.exempel.sds.security.OrganisationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class ArendeService {

    private final ArendeRepository arendeRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public ArendeService(ArendeRepository arendeRepository,
                       AuthorizationService authorizationService,
                       AuditService auditService) {
        this.arendeRepository = arendeRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ArendeResponse getArende(UUID id, OrganisationPrincipal principal) {
        Arende dokumentArende = requireArende(id, principal.organisationId());
        authorizationService.assertReadAccess(dokumentArende, principal);
        auditService.log(AuditAction.ARENDE_READ, principal, id);
        return ArendeResponse.from(dokumentArende);
    }

    @Transactional(readOnly = true)
    public List<ArendeResponse> listArenden(OrganisationPrincipal principal, ArendeStatus status) {
        List<Arende> arenden;

        if (status == null) {
            arenden = arendeRepository.findAllByOrganisationId(principal.organisationId());
        } else {
            arenden = arendeRepository.findAllByOrganisationIdAndStatus(principal.organisationId(), status);
        }

        return arenden.stream()
                .map(ArendeResponse::from)
                .toList();
    }

    public ArendeResponse createArende(CreateArendeRequest request, OrganisationPrincipal principal) {
        Arende dokumentArende = Arende.builder()
                .organisationId(principal.organisationId())
                .title(request.title())
                .description(request.description())
                .status(ArendeStatus.OPEN)
                .createdBy(principal.userId())
                .build();

        ArendeResponse response = ArendeResponse.from(arendeRepository.save(dokumentArende));
        auditService.log(AuditAction.ARENDE_CREATE, principal, response.id());
        return response;
    }

    public ArendeResponse closeArende(UUID id, OrganisationPrincipal principal) {
        Arende dokumentArende = requireArende(id, principal.organisationId());
        authorizationService.assertCloseAccess(dokumentArende, principal);

        if (dokumentArende.getStatus() == ArendeStatus.CLOSED) {
            throw new IllegalStateException("Arende is already CLOSED");
        }

        dokumentArende.setStatus(ArendeStatus.CLOSED);
        ArendeResponse response = ArendeResponse.from(arendeRepository.save(dokumentArende));
        auditService.log(AuditAction.ARENDE_CLOSED, principal, id);
        return response;
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Arende requireArende(UUID id, String organisationId) {
        return arendeRepository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Arende not found: " + id));
    }
}
