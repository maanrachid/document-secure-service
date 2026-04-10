package se.exempel.sds.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Immutable principal attached to every authenticated request.
 * Stored as the principal in UsernamePasswordAuthenticationToken so it is
 * reachable via SecurityContextHolder without hitting the database.
 */
public record OrganisationPrincipal(
        String userId,
        String organisationId,
        Collection<GrantedAuthority> authorities) {

    public OrganisationPrincipal {
        authorities = List.copyOf(authorities);
    }

    /**
     * Returns true when the principal holds the given role.
     * Matches against Spring's "ROLE_" prefix convention, so
     * {@code hasRole("ADMIN")} matches the authority {@code "ROLE_ADMIN"}.
     */
    public boolean hasRole(String role) {
        String prefixed = "ROLE_" + role;
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(prefixed::equals);
    }
}
