package se.exempel.sds.dto;

import se.exempel.sds.domain.Classification;
import se.exempel.sds.domain.Dokument;

import java.time.Instant;
import java.util.UUID;

public record DokumentResponse(
        UUID id,
        String organisationId,
        String title,
        String description,
        Classification classification,
        UUID arendeId,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static DokumentResponse from(Dokument doc) {
        UUID arendeId = doc.getLinkedArende() != null
                ? doc.getLinkedArende().getId()
                : null;

        return new DokumentResponse(
                doc.getId(),
                doc.getOrganisationId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getClassification(),
                arendeId,
                doc.getCreatedBy(),
                doc.getCreatedAt(),
                doc.getUpdatedBy(),
                doc.getUpdatedAt()
        );
    }
}
