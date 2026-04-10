package se.exempel.sds.dto;

import se.exempel.sds.domain.Classification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDokumentRequest(

        @NotBlank
        String title,

        String description,

        @NotNull
        Classification classification,

        UUID arendeId
) {}
