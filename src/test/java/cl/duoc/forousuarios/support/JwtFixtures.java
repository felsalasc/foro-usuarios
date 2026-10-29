package cl.duoc.forousuarios.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public final class JwtFixtures {
    public static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private JwtFixtures() { }

    public static String token(String subject, Instant issuedAt, Instant expiresAt, String issuer, List<String> audience)
            throws JOSEException {
        var claims = new JWTClaimsSet.Builder().subject(subject).issuer(issuer).audience(audience);
        if (issuedAt != null) claims.issueTime(Date.from(issuedAt));
        if (expiresAt != null) claims.expirationTime(Date.from(expiresAt));
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        jwt.sign(new MACSigner(Base64.getDecoder().decode(SECRET)));
        return jwt.serialize();
    }

    public static String firmaManipulada(String token) {
        var parts = token.split("\\.");
        parts[2] = (parts[2].startsWith("a") ? "b" : "a") + parts[2].substring(1);
        return String.join(".", parts);
    }
}
