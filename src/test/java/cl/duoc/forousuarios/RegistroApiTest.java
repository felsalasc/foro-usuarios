package cl.duoc.forousuarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class RegistroApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registroPersisteHashYDevuelve201() throws Exception {
        var response = mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(body(" Ana Pérez ", " ANA@EXAMPLE.TEST ", "ClavePrueba123")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.correo").value("ana@example.test"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andReturn().getResponse();
        var json = mapper.readTree(response.getContentAsString());
        assertThat(json.size()).isEqualTo(4);
        long id = json.get("id").asLong();
        assertThat(id).isPositive();
        assertThat(response.getHeader("Location")).endsWith("/api/usuarios/" + id);
        String hash = jdbc.queryForObject("SELECT PASSWORD_HASH FROM USUARIOS WHERE ID = ?", String.class, id);
        assertThat(hash).isNotEqualTo("ClavePrueba123").hasSize(60);
        assertThat(new BCryptPasswordEncoder().matches("ClavePrueba123", hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT ACTIVO FROM USUARIOS WHERE ID = ?", Integer.class, id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rol", "id", "activo", "passwordHash"})
    void registroRechazaCamposControladosPorServidor(String campo) throws Exception {
        var datos = new HashMap<String, Object>(Map.of("nombre", "Ana", "correo", "ana@example.test", "password", "ClavePrueba123"));
        datos.put(campo, campo.equals("id") ? 999 : "ADMINISTRADOR");
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
        assertThat(cantidad()).isZero();
    }

    @Test
    void correoNormalizadoEsUnico() throws Exception {
        registrar("ana@example.test");
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(body("Otra Ana", " ANA@EXAMPLE.TEST ", "OtraClave123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        assertThat(cantidad()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void registroConcurrenteCorreoUnico() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var requestBody = body("Concurrente", "concurrente@example.test", "ClavePrueba123");
            java.util.concurrent.Callable<Integer> tarea = () -> {
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No se inició la prueba concurrente");
                return mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON).content(requestBody))
                        .andReturn().getResponse().getStatus();
            };
            var a = pool.submit(tarea);
            var b = pool.submit(tarea);
            inicio.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS WHERE CORREO = ?", Long.class,
                    "concurrente@example.test")).isEqualTo(1);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
            jdbc.update("DELETE FROM USUARIOS WHERE CORREO = ?", "concurrente@example.test");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "correo", "password"})
    void obligatoriosInvalidos(String campo) throws Exception {
        for (String valor : new String[]{null, "", "   "}) {
            var datos = new HashMap<String, Object>(Map.of("nombre", "Ana", "correo", "ana@example.test", "password", "ClavePrueba123"));
            datos.put(campo, valor);
            mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(datos)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors." + campo).exists());
            datos.remove(campo);
            mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(datos)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(cantidad()).isZero();
    }

    @Test
    void rechazaNombreLargoYCorreoInvalido() throws Exception {
        for (String json : List.of(body("x".repeat(101), "ana@example.test", "ClavePrueba123"),
                body("Ana", "no-es-correo", "ClavePrueba123"),
                body("Ana", "a".repeat(243) + "@example.test", "ClavePrueba123"))) {
            mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        assertThat(cantidad()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "1234567890123456789012345678901234567890123456789012345678901234567890123", "ááááááááááááááááááááááááááááááááááááá"})
    void rechazaPasswordFueraDeLimitesUtf8(String password) throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(body("Ana", "ana@example.test", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        assertThat(cantidad()).isZero();
    }

    @Test
    void aceptaPasswordDe72BytesSinTruncarlo() throws Exception {
        String password = "á".repeat(36);
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(body("Ana", "ana@example.test", password)))
                .andExpect(status().isCreated());
        String hash = jdbc.queryForObject("SELECT PASSWORD_HASH FROM USUARIOS WHERE CORREO = ?", String.class, "ana@example.test");
        assertThat(new BCryptPasswordEncoder().matches(password, hash)).isTrue();
    }

    @Test
    void passwordsIgualesTienenHashesDistintos() throws Exception {
        registrar("ana@example.test");
        registrar("otra@example.test");
        var hashes = jdbc.queryForList("SELECT PASSWORD_HASH FROM USUARIOS", String.class);
        assertThat(hashes).hasSize(2).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null",
            "{\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"password\":12345678}",
            "{\"nombre\":true,\"correo\":\"ana@example.test\",\"password\":\"ClavePrueba123\"}",
            "{\"nombre\":12.5,\"correo\":\"ana@example.test\",\"password\":\"ClavePrueba123\"}"})
    void rechazaJsonMalformadoOTiposIncorrectos(String json) throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        assertThat(cantidad()).isZero();
    }

    private String body(String nombre, String correo, String password) throws Exception {
        return mapper.writeValueAsString(Map.of("nombre", nombre, "correo", correo, "password", password));
    }

    private void registrar(String correo) throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(body("Ana", correo, "ClavePrueba123")))
                .andExpect(status().isCreated());
    }

    private long cantidad() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS", Long.class);
    }
}
