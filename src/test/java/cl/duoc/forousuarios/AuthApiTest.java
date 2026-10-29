package cl.duoc.forousuarios;

import cl.duoc.forousuarios.support.JwtFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Transactional
class AuthApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void loginEmiteTokenYMeDevuelveIdentidad() throws Exception {
        long id = registrar();
        var response = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(loginBody(" ANA@EXAMPLE.TEST ", "ClavePrueba123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.usuario.id").value(id))
                .andReturn().getResponse();
        var json = mapper.readTree(response.getContentAsString());
        assertThat(json.size()).isEqualTo(4);
        assertThat(json.get("usuario").size()).isEqualTo(4);
        assertThat(response.getHeader("Set-Cookie")).isNull();
        String token = json.get("accessToken").asText();
        assertThat(token.split("\\.")).hasSize(3);
        var me = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.correo").value("ana@example.test"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andReturn().getResponse();
        assertThat(mapper.readTree(me.getContentAsString()).size()).isEqualTo(4);
    }

    @Test
    void credencialesIncorrectasSonGenericas() throws Exception {
        long id = registrar();
        var incorrecta = loginError("ana@example.test", "Incorrecta123");
        var ausente = loginError("ausente@example.test", "ClavePrueba123");
        jdbc.update("UPDATE USUARIOS SET ACTIVO = 0 WHERE ID = ?", id);
        entityManager.clear();
        var inactiva = loginError("ana@example.test", "ClavePrueba123");
        assertThat(ausente.get("detail")).isEqualTo(incorrecta.get("detail"));
        assertThat(inactiva.get("detail")).isEqualTo(incorrecta.get("detail"));
        assertThat(incorrecta.toString()).doesNotContain("Incorrecta123", "PASSWORD_HASH", "ana@example.test");
    }

    @Test
    void cuentaDesactivadaPierdeAccesoConTokenAnterior() throws Exception {
        long id = registrar();
        String token = login();
        jdbc.update("UPDATE USUARIOS SET ACTIVO = 0 WHERE ID = ?", id);
        entityManager.clear();
        esperar401(token);
    }

    @Test
    void meDevuelveRolVigente() throws Exception {
        long id = registrar();
        String token = login();
        jdbc.update("UPDATE USUARIOS SET ROL = 'ADMINISTRADOR' WHERE ID = ?", id);
        entityManager.clear();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void tokenDeCuentaEliminadaNoAutoriza() throws Exception {
        long id = registrar();
        String token = login();
        jdbc.update("DELETE FROM USUARIOS WHERE ID = ?", id);
        entityManager.clear();
        esperar401(token);
    }

    @Test
    void sinTokenEs401ConProblemDetail() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-es-jwt", "Bearer", "", "Basic abc"})
    void cabeceraInvalidaEs401(String header) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", header))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void firmaManipuladaEs401() throws Exception {
        registrar();
        esperar401(JwtFixtures.firmaManipulada(login()));
    }

    @Test
    void claimsInvalidosSon401() throws Exception {
        long id = registrar();
        Instant now = Instant.now();
        var validSubject = String.valueOf(id);
        for (String token : List.of(
                JwtFixtures.token(validSubject, now.minusSeconds(1000), now.minusSeconds(60), "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token(validSubject, now, null, "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token(validSubject, now, now.plusSeconds(900), "otro", List.of("foro-api")),
                JwtFixtures.token(validSubject, now, now.plusSeconds(900), "foro-usuarios", List.of("otro")),
                JwtFixtures.token(validSubject, now, now.plusSeconds(900), "foro-usuarios", null),
                JwtFixtures.token(null, now, now.plusSeconds(900), "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token("abc", now, now.plusSeconds(900), "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token("0", now, now.plusSeconds(900), "foro-usuarios", List.of("foro-api")),
                JwtFixtures.token("99999999", now, now.plusSeconds(900), "foro-usuarios", List.of("foro-api")))) {
            esperar401(token);
        }
    }

    @Test
    void loginNoTruncaPasswordSuperiorA72Bytes() throws Exception {
        String password = "á".repeat(36);
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nombre", "Ana", "correo", "ana@example.test", "password", password))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(loginBody("ana@example.test", password + "x")))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{", "{\"correo\":\"ana@example.test\",\"password\":12345678}",
            "{\"correo\":\"ana@example.test\",\"password\":\"ClavePrueba123\",\"rol\":\"ADMINISTRADOR\"}"})
    void loginInvalidoEs400(String body) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    private long registrar() throws Exception {
        var result = mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nombre", "Ana", "correo", "ana@example.test", "password", "ClavePrueba123"))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return mapper.readTree(result.getContentAsString()).get("id").asLong();
    }

    private String login() throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(loginBody("ana@example.test", "ClavePrueba123")))
                .andExpect(status().isOk()).andReturn().getResponse();
        return mapper.readTree(result.getContentAsString()).get("accessToken").asText();
    }

    private com.fasterxml.jackson.databind.JsonNode loginError(String correo, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(loginBody(correo, password)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse();
        return mapper.readTree(result.getContentAsString());
    }

    private String loginBody(String correo, String password) throws Exception {
        return mapper.writeValueAsString(Map.of("correo", correo, "password", password));
    }

    private void esperar401(String token) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
