package cl.duoc.forousuarios.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class JwtService {
    private final Clock clock;
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    public JwtService(@Value("${security.jwt.secret-base64}") String secretBase64, Clock clock) {
        this.clock = clock;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("JWT_SECRET_BASE64 debe ser Base64 válido");
        }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET_BASE64 requiere al menos 32 bytes");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamp.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator("foro-usuarios"), this::validarClaims));
    }

    public String emitir(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El identificador debe ser positivo");
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer("foro-usuarios").audience(List.of("foro-api"))
                .subject(id.toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Long validar(String token) {
        Jwt jwt = decoder.decode(token);
        try {
            long id = Long.parseLong(jwt.getSubject());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new BadJwtException("Sujeto inválido");
        }
    }

    private OAuth2TokenValidatorResult validarClaims(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();
        if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)
                || issuedAt.isAfter(clock.instant().plusSeconds(30))
                || jwt.getAudience() == null || !jwt.getAudience().contains("foro-api")) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Claims inválidos", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
