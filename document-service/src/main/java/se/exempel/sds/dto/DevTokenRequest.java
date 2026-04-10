package se.exempel.sds.dto;

import java.util.List;

/**
 * Only used in the dev profile.
 */
public record DevTokenRequest(
        String userId,
        String organisationId,
        List<String> roles) {}
