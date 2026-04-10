package se.exempel.sds.service;

import se.exempel.sds.security.OrganisationPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static se.exempel.sds.service.AuditService.sanitize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for the log-injection protection in {@link AuditService}.
 * <p>
 * The sanitize() method is tested directly (package-private) because
 * verifying that a specific string appears in a log file is fragile and
 * brittle. The security guarantee is that control characters are stripped.
 */
class AuditServiceTest {

    private final AuditService auditService = new AuditService();

    // ── sanitize – control-character stripping ────────────────────────────────

    @Test
    void sanitize_shouldReplaceCR() {
        assertThat(sanitize("user\rINJECTED")).isEqualTo("user_INJECTED");
    }

    @Test
    void sanitize_shouldReplaceLF() {
        assertThat(sanitize("user\nINFO: FAKE_LOG_ENTRY")).isEqualTo("user_INFO: FAKE_LOG_ENTRY");
    }

    @Test
    void sanitize_shouldReplaceCRLF() {
        assertThat(sanitize("user\r\nINJECTED")).isEqualTo("user__INJECTED");
    }

    @Test
    void sanitize_shouldReplaceTab() {
        assertThat(sanitize("user\tvalue")).isEqualTo("user_value");
    }

    @Test
    void sanitize_shouldReplaceNullByte() {
        assertThat(sanitize("user\u0000evil")).isEqualTo("user_evil");
    }

    @Test
    void sanitize_shouldReplaceDelCharacter() {
        // DEL (U+007F) is the last ASCII control character and is in \p{Cntrl}
        assertThat(sanitize("user\u007Fevil")).isEqualTo("user_evil");
    }

    @Test
    void sanitize_shouldReplaceMultipleControlChars_inOneCall() {
        assertThat(sanitize("a\rb\nc\u0000d")).isEqualTo("a_b_c_d");
    }

    @Test
    void sanitize_shouldReturnPlaceholder_forNullInput() {
        assertThat(sanitize(null)).isEqualTo("(null)");
    }

    @Test
    void sanitize_shouldHandle_emptyString() {
        assertThat(sanitize("")).isEqualTo("");
    }

    @Test
    void sanitize_shouldPassThrough_normalStrings() {
        String normal = "organisation-abc_123";
        assertThat(sanitize(normal)).isEqualTo(normal);
    }

    @Test
    void sanitize_shouldPassThrough_uuids() {
        String uuid = UUID.randomUUID().toString();
        assertThat(sanitize(uuid)).isEqualTo(uuid);
    }

    @Test
    void sanitize_shouldPassThrough_unicodeChars() {
        // Swedish characters are NOT control chars and must not be mangled
        String swedish = "Åsa från Örebro";
        assertThat(sanitize(swedish)).isEqualTo(swedish);
    }

    // ── log / logAccessDenied – smoke tests ──────────────────────────────────

    @Test
    void log_shouldNotThrow_forValidInputs() {
        OrganisationPrincipal principal = new OrganisationPrincipal(
                "user-123", "organisation-A",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThatNoException().isThrownBy(() ->
                auditService.log(AuditAction.DOKUMENT_READ, principal, UUID.randomUUID()));
    }

    @Test
    void logAccessDenied_shouldNotThrow_forNullPrincipal() {
        // Covers unauthenticated requests (missing or invalid JWT)
        assertThatNoException().isThrownBy(() ->
                auditService.logAccessDenied(null, "/api/dokuments/secret"));
    }

    @Test
    void logAccessDenied_shouldNotThrow_whenResourceContainsCRLF() {
        // Attacker-controlled URI should not break the log line
        OrganisationPrincipal principal = new OrganisationPrincipal(
                "user\r\nevil", "organisation-A", List.of());

        assertThatNoException().isThrownBy(() ->
                auditService.logAccessDenied(principal, "/api\r\nINJECTED=true"));
    }

    @Test
    void logAccessDenied_shouldNotThrow_forAuthenticatedPrincipalWithoutRoles() {
        OrganisationPrincipal principal = new OrganisationPrincipal(
                "user-1", "organisation-A", List.of());

        assertThatNoException().isThrownBy(() ->
                auditService.logAccessDenied(principal, "/api/dokuments/123"));
    }
}
