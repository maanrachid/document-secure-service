package se.exempel.sds.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateArendeRequest(

        @NotBlank
        String title,

        String description
) {}
