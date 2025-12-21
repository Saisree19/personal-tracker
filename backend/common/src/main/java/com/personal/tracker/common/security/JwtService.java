package com.personal.tracker.common.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public Mono<TokenResult> issueToken(String subject, List<String> roles) {
        return Mono.fromSupplier(() -> buildToken(subject, roles));
    }

    private TokenResult buildToken(String subject, List<String> roles) {
        Instant now = Instant.now();
        Instant expires = now.plus(properties.ttl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expires)
                .claim("roles", roles)
                .build();

        JwtEncoderParameters params = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
        String tokenValue = jwtEncoder.encode(params).getTokenValue();
        return new TokenResult(tokenValue, expires);
    }

    public record TokenResult(String token, Instant expiresAt) {
    }
}
