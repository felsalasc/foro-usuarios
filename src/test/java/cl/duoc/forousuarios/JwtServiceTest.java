package cl.duoc.forousuarios;

import cl.duoc.forousuarios.security.JwtService;
import cl.duoc.forousuarios.support.JwtFixtures;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private final JwtService service = serviceAt(NOW);

    @Test
    void emiteTokenFirmadoConDuracionDe900Segundos() throws Exception {
        String token = service.emitir(7L);
        var parsed = SignedJWT.parse(token);
        assertThat(parsed.getHeader().getAlgorithm().getName()).isEqualTo("HS256");
        var claims = parsed.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("foro-usuarios");
        assertThat(claims.getAudience()).containsExactly("foro-api");
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(NOW.plusSeconds(900));
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(NOW);
        assertThat(service.validar(token)).isEqualTo(7L);
    }

    @Test
    void toleranciaTemporalDe30SegundosSinEsperas() {
        String token = service.emitir(7L);
        assertThat(serviceAt(NOW.plusSeconds(929)).validar(token)).isEqualTo(7L);
        assertThatThrownBy(() -> serviceAt(NOW.plusSeconds(931)).validar(token)).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "!no-base64", "YQ=="})
    void rechazaClavesInvalidasOCortas(String key) {
        assertThatThrownBy(() -> new JwtService(key, Clock.fixed(NOW, ZoneOffset.UTC)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaFirmaManipulada() {
        String token = JwtFixtures.firmaManipulada(service.emitir(7L));
        assertThatThrownBy(() -> service.validar(token)).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "9223372036854775808", " 7"})
    void rechazaSujetosInvalidos(String subject) throws Exception {
        String token = JwtFixtures.token(subject, NOW, NOW.plusSeconds(900), "foro-usuarios", List.of("foro-api"));
        assertThatThrownBy(() -> service.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void requiereExpIatIssuerYAudienceValidos() throws Exception {
        for (String token : List.of(
                JwtFixtures.token("7", NOW, null, "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token("7", null, NOW.plusSeconds(900), "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token("7", NOW, NOW.plusSeconds(900), "otro", List.of("foro-api")),
                JwtFixtures.token("7", NOW, NOW.plusSeconds(900), "foro-usuarios", List.of("otro")),
                JwtFixtures.token("7", NOW.plusSeconds(120), NOW.plusSeconds(900), "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token("7", NOW, NOW.minusSeconds(1), "foro-usuarios", List.of("foro-api")))) {
            assertThatThrownBy(() -> service.validar(token)).isInstanceOf(JwtException.class);
        }
    }

    @Test
    void audienciaAusenteEsTokenInvalido() throws Exception {
        String token = JwtFixtures.token("7", NOW, NOW.plusSeconds(900), "foro-usuarios", null);
        assertThatThrownBy(() -> service.validar(token)).isInstanceOf(JwtException.class);
    }

    private JwtService serviceAt(Instant now) {
        return new JwtService(JwtFixtures.SECRET, Clock.fixed(now, ZoneOffset.UTC));
    }
}
