package se.exempel.sds.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.exempel.sds.dto.DevTokenRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Dev-only endpoint for generating signed JWT tokens.
 * Only active when the {@code dev} Spring profile is enabled – does not exist in production.
 */
@Profile("dev")
@RestController
@RequestMapping("/dev")
public class DevTokenController {

    private final byte[] secretBytes;

    public DevTokenController(@Value("${app.dev.jwt-secret}") String secret) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/token")
    public Map<String, String> generateToken(@RequestBody DevTokenRequest request)
            throws JOSEException {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(request.userId())
                .claim("organisation_id", request.organisationId())
                .claim("roles", request.roles() != null ? request.roles() : List.of())
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(8, ChronoUnit.HOURS)))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(secretBytes));

        return Map.of("token", signedJWT.serialize());
    }
}
