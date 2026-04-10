package se.exempel.sds.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Disabled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// UPPGIFT A – Ta bort @Disabled när du har implementerat JwtOrganisationConverter.convert().
// Alla tester i den här klassen ska vara gröna när uppgiften är klar.
//@Disabled("Uppgift A ej implementerad")
class JwtOrganisationConverterTest {

    private final JwtOrganisationConverter converter = new JwtOrganisationConverter();

    @AfterEach
    void cleanUp() {
        // Simulate the cleanup filter so tests don't leak state into each other
        OrganisationContext.clear();
    }

    // ── Happy-path extraction ─────────────────────────────────────────────────

    @Test
    void convert_shouldExtractOrganisationIdAndUserId() {
        Jwt jwt = jwt("user-123", List.of("USER"));

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) converter.convert(jwt);

        OrganisationPrincipal principal = (OrganisationPrincipal) token.getPrincipal();
        assertThat(Objects.requireNonNull(principal).userId()).isEqualTo("user-123");
        assertThat(principal.organisationId()).isEqualTo("organisation-A");
    }

    @Test
    void convert_shouldMapRolesToGrantedAuthorities() {
        Jwt jwt = jwt("user-1", List.of("USER", "ADMIN"));

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) converter.convert(jwt);

        assertThat(token.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void convert_shouldUseJwtAsCredentials() {
        // The raw JWT must be stored as credentials so downstream code can inspect claims
        Jwt jwt = jwt("user-1", List.of("USER"));

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) converter.convert(jwt);
        assertThat(token.getCredentials()).isSameAs(jwt);
    }

    @Test
    void convert_shouldHandleMissingRolesClaim_withEmptyAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("organisation_id", "organisation-A")
                // no "roles" claim
                .build();

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    // ── Missing / invalid organisation_id ────────────────────────────────────

    @Test
    void convert_shouldThrow_whenOrganisationIdClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                // no "organisation_id" claim
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("organisation_id");
    }

    @Test
    void convert_shouldThrow_whenOrganisationIdClaimIsBlank() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("organisation_id", "   ")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void convert_shouldThrow_whenOrganisationIdClaimIsEmptyString() {
        // "" is also blank – must be rejected the same way as whitespace-only
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("organisation_id", "")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── OrganisationContext population ────────────────────────────────────────

    @Test
    void convert_shouldPopulateOrganisationContext() {
        Jwt jwt = jwt("user-1", List.of("USER"));

        converter.convert(jwt);

        OrganisationPrincipal fromContext = OrganisationContext.get();
        assertThat(fromContext.userId()).isEqualTo("user-1");
        assertThat(fromContext.organisationId()).isEqualTo("organisation-A");
    }

    @Test
    void clear_shouldMakeContextUnavailable() {
        // Confirms that the cleanup filter leaves no state between requests
        converter.convert(jwt("user-1", List.of("USER")));
        OrganisationContext.clear();

        assertThatThrownBy(OrganisationContext::get)
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static Jwt jwt(String subject, List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of(
                        "organisation_id", "organisation-A",
                        "roles", roles
                )))
                .build();
    }
}
