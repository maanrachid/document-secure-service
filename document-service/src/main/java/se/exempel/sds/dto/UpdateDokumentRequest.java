package se.exempel.sds.dto;

import se.exempel.sds.domain.Classification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDokumentRequest(

        @NotBlank
        String title,

        String description,

        @NotNull
        Classification classification
) {}
