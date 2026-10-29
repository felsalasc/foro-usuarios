package cl.duoc.forousuarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class AdministradorConcurrenteTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    long a;
    long b;
    String tokenA;
    String tokenB;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        a = registrar("concurr-admin-a@example.test");
        b = registrar("concurr-admin-b@example.test");
        jdbc.update("UPDATE USUARIOS SET ROL = 'ADMINISTRADOR' WHERE ID IN (?, ?)", a, b);
        tokenA = login("concurr-admin-a@example.test");
        tokenB = login("concurr-admin-b@example.test");
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM USUARIOS WHERE CORREO LIKE 'concurr-admin-%'");
    }

    @Test
    void conservaAdministradorConcurrente() throws Exception {
        var statuses = paralelo(
                () -> mvc.perform(delete("/api/usuarios/" + a).header("Authorization", "Bearer " + tokenA)).andReturn().getResponse().getStatus(),
                () -> mvc.perform(delete("/api/usuarios/" + b).header("Authorization", "Bearer " + tokenB)).andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(204, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS WHERE ROL = 'ADMINISTRADOR' AND ACTIVO = 1", Long.class)).isEqualTo(1);
    }

    @Test
    void editarPerfilNoRestauraUnRolCambiadoConcurrentemente() throws Exception {
        String perfil = mapper.writeValueAsString(Map.of("nombre", "Nombre actualizado", "correo", "concurr-admin-a@example.test"));
        var statuses = paralelo(
                () -> mvc.perform(put("/api/usuarios/" + a).header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content(perfil)).andReturn().getResponse().getStatus(),
                () -> mvc.perform(put("/api/usuarios/" + a + "/rol").header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content("{\"rol\":\"USUARIO\"}")).andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactly(200, 200);
        assertThat(jdbc.queryForObject("SELECT ROL FROM USUARIOS WHERE ID = ?", String.class, a)).isEqualTo("USUARIO");
        assertThat(jdbc.queryForObject("SELECT NOMBRE FROM USUARIOS WHERE ID = ?", String.class, a)).isEqualTo("Nombre actualizado");
    }

    @Test
    void dosDespromocionesNoQuitanElUltimoAdministrador() throws Exception {
        var statuses = paralelo(
                () -> mvc.perform(put("/api/usuarios/" + a + "/rol").header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON).content("{\"rol\":\"USUARIO\"}")).andReturn().getResponse().getStatus(),
                () -> mvc.perform(put("/api/usuarios/" + b + "/rol").header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content("{\"rol\":\"USUARIO\"}")).andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS WHERE ROL = 'ADMINISTRADOR' AND ACTIVO = 1", Long.class)).isEqualTo(1);
    }

    @Test
    void desactivacionYDespromocionConcurrentesConservanAdministrador() throws Exception {
        var statuses = paralelo(
                () -> mvc.perform(delete("/api/usuarios/" + a).header("Authorization", "Bearer " + tokenA)).andReturn().getResponse().getStatus(),
                () -> mvc.perform(put("/api/usuarios/" + b + "/rol").header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON).content("{\"rol\":\"USUARIO\"}")).andReturn().getResponse().getStatus());
        assertThat(statuses).contains(409);
        assertThat(statuses.stream().filter(s -> s >= 200 && s < 300).count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS WHERE ROL = 'ADMINISTRADOR' AND ACTIVO = 1", Long.class)).isEqualTo(1);
    }

    private List<Integer> paralelo(Callable<Integer> a, Callable<Integer> b) throws Exception {
        var inicio = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var fa = pool.submit(() -> { inicio.await(); return a.call(); });
            var fb = pool.submit(() -> { inicio.await(); return b.call(); });
            inicio.countDown();
            return List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private long registrar(String correo) throws Exception {
        var response = mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("nombre", "Admin", "correo", correo, "password", "ClavePrueba123"))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString()).get("id").asLong();
    }

    private String login(String correo) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("correo", correo, "password", "ClavePrueba123"))))
                .andExpect(status().isOk()).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString()).get("accessToken").asText();
    }
}
