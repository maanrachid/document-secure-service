package se.exempel.sds.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Converts a validated {@link Jwt} into a Spring Security authentication token
 * that carries a {@link OrganisationPrincipal} as its principal.
 */
public class JwtOrganisationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        //
        // TODO (Uppgift A)
        //
        // Bygg ett UsernamePasswordAuthenticationToken med en OrganisationPrincipal som principal.
        //
        // Claims att läsa från jwt:
        //   jwt.getSubject()                       – användarens ID  (sub-claim)
        //   jwt.getClaim("organisation_id")         – organisationens ID (obligatorisk)
        //   jwt.getClaimAsStringList("roles")       – lista med rollnamn, t.ex. ["USER", "ADMIN"]
        //
        // Önskat resultat:
        //   - Saknas eller är organisation_id tom ska BadCredentialsException kastas.
        //   - Roller mappas till GrantedAuthority med prefixet "ROLE_"
        //     t.ex. "ADMIN" → new SimpleGrantedAuthority("ROLE_ADMIN")
        //   - Anropa OrganisationContext.set(principal) efter att principal skapats.
        //   - Returnera new UsernamePasswordAuthenticationToken(principal, jwt, authorities)

        String userId = jwt.getSubject();
        String organisationId = jwt.getClaim("organisation_id");

        if (organisationId == null || organisationId.trim().isEmpty()) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Missing or empty organisation_id claim");
        }

        java.util.List<String> roles = jwt.getClaimAsStringList("roles");
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = java.util.Optional
                .ofNullable(roles)
                .orElse(java.util.Collections.emptyList())
                .stream()
                .map(role -> (org.springframework.security.core.GrantedAuthority)
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .collect(java.util.stream.Collectors.toList());

        OrganisationPrincipal principal = new OrganisationPrincipal(userId, organisationId, authorities);
        OrganisationContext.set(principal);

        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
