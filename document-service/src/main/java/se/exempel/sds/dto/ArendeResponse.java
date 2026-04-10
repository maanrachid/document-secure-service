package se.exempel.sds.dto;

import se.exempel.sds.domain.ArendeStatus;
import se.exempel.sds.domain.Arende;

import java.time.Instant;
import java.util.UUID;

public record ArendeResponse(
        UUID id,
        String organisationId,
        String title,
        String description,
        ArendeStatus status,
        String createdBy,
        Instant createdAt
) {
    public static ArendeResponse from(Arende c) {
        return new ArendeResponse(
                c.getId(),
                c.getOrganisationId(),
                c.getTitle(),
                c.getDescription(),
                c.getStatus(),
                c.getCreatedBy(),
                c.getCreatedAt()
        );
    }
}
