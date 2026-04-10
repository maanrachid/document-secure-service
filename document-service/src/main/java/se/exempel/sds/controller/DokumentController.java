package se.exempel.sds.controller;

import se.exempel.sds.dto.CreateDokumentRequest;
import se.exempel.sds.dto.DokumentResponse;
import se.exempel.sds.dto.UpdateDokumentRequest;
import se.exempel.sds.security.OrganisationPrincipal;
import se.exempel.sds.service.DokumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dokuments")
public class DokumentController {

    private final DokumentService dokumentService;

    public DokumentController(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    @GetMapping
    public List<DokumentResponse> listDokuments(
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return dokumentService.listDokuments(principal);
    }

    @GetMapping("/by-arende/{arendeId}")
    public List<DokumentResponse> listDokumentsByArende(
            @PathVariable UUID arendeId,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return dokumentService.listDokumentsByArende(arendeId, principal);
    }

    @GetMapping("/{id}")
    public DokumentResponse getDokument(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return dokumentService.getDokument(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DokumentResponse createDokument(
            @RequestBody @Valid CreateDokumentRequest request,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return dokumentService.createDokument(request, principal);
    }

    @PutMapping("/{id}")
    public DokumentResponse updateDokument(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDokumentRequest request,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        return dokumentService.updateDokument(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDokument(
            @PathVariable UUID id,
            @AuthenticationPrincipal OrganisationPrincipal principal) {
        dokumentService.deleteDokument(id, principal);
    }
}
