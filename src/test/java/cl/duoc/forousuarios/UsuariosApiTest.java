package cl.duoc.forousuarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Transactional
class UsuariosApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    long adminId;
    long usuarioId;
    long otroId;
    String adminToken;
    String usuarioToken;

    @BeforeEach
    void cuentas() throws Exception {
        adminId = registrar("admin@example.test");
        usuarioId = registrar("usuario@example.test");
        otroId = registrar("otro@example.test");
        jdbc.update("UPDATE USUARIOS SET ROL = 'ADMINISTRADOR' WHERE ID = ?", adminId);
        em.clear();
        adminToken = login("admin@example.test", "ClavePrueba123", 200);
        usuarioToken = login("usuario@example.test", "ClavePrueba123", 200);
    }

    @Test
    void usuarioNoPuedeGestionarOtraCuenta() throws Exception {
        mvc.perform(get("/api/usuarios/" + otroId).header("Authorization", bearer(usuarioToken)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(put("/api/usuarios/" + otroId).header("Authorization", bearer(usuarioToken))
                        .contentType(APPLICATION_JSON).content(datos("Cambio", "otro@example.test")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + otroId).header("Authorization", bearer(usuarioToken)))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT NOMBRE FROM USUARIOS WHERE ID = ?", String.class, otroId)).isEqualTo("Cuenta");
        assertThat(jdbc.queryForObject("SELECT ACTIVO FROM USUARIOS WHERE ID = ?", Integer.class, otroId)).isEqualTo(1);
    }

    @Test
    void administradorPuedeListarSinSecretos() throws Exception {
        var response = mvc.perform(get("/api/usuarios").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse();
        for (var usuario : mapper.readTree(response.getContentAsString())) assertThat(usuario.size()).isEqualTo(4);
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(usuarioToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioConsultaSuCuenta() throws Exception {
        mvc.perform(get("/api/usuarios/" + usuarioId).header("Authorization", bearer(usuarioToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuarioId));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usuarioPuedeEditarSuCuentaSinCambiarHash(boolean passwordNulo) throws Exception {
        String hash = hash(usuarioId);
        var body = mapper.createObjectNode().put("nombre", " Nombre nuevo ").put("correo", " NUEVO@EXAMPLE.TEST ");
        if (passwordNulo) body.putNull("password");
        mvc.perform(put("/api/usuarios/" + usuarioId).header("Authorization", bearer(usuarioToken))
                        .contentType(APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nombre nuevo"))
                .andExpect(jsonPath("$.correo").value("nuevo@example.test"))
                .andExpect(jsonPath("$.rol").value("USUARIO"));
        assertThat(hash(usuarioId)).isEqualTo(hash);
        login("nuevo@example.test", "ClavePrueba123", 200);
        login("usuario@example.test", "ClavePrueba123", 401);
    }

    @Test
    void actualizarPasswordCambiaCredencialesYConservaTokenVigente() throws Exception {
        mvc.perform(put("/api/usuarios/" + usuarioId).header("Authorization", bearer(usuarioToken))
                        .contentType(APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                                "nombre", "Cuenta", "correo", "usuario@example.test", "password", "NuevaClave123"))))
                .andExpect(status().isOk());
        login("usuario@example.test", "ClavePrueba123", 401);
        login("usuario@example.test", "NuevaClave123", 200);
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(usuarioToken))).andExpect(status().isOk());
    }

    @Test
    void usuarioPuedeDesactivarSuCuenta() throws Exception {
        mvc.perform(delete("/api/usuarios/" + usuarioId).header("Authorization", bearer(usuarioToken)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        em.clear();
        assertThat(jdbc.queryForObject("SELECT ACTIVO FROM USUARIOS WHERE ID = ?", Integer.class, usuarioId)).isZero();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(usuarioToken))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/" + usuarioId).header("Authorization", bearer(adminToken))).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void administradorPuedeGestionarYDesactivarOtraCuenta() throws Exception {
        mvc.perform(put("/api/usuarios/" + usuarioId).header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON).content(datos("Editada por admin", "usuario@example.test")))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/usuarios/" + usuarioId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        em.clear();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(usuarioToken))).andExpect(status().isUnauthorized());
    }

    @Test
    void rolSoloLoCambiaAdministrador() throws Exception {
        String body = "{\"rol\":\"ADMINISTRADOR\"}";
        mvc.perform(put("/api/usuarios/" + usuarioId + "/rol").header("Authorization", bearer(usuarioToken))
                        .contentType(APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + usuarioId + "/rol").header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
        em.clear();
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(usuarioToken))).andExpect(status().isOk());
    }

    @Test
    void conservaUltimoAdministrador() throws Exception {
        mvc.perform(delete("/api/usuarios/" + adminId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(put("/api/usuarios/" + adminId + "/rol").header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON).content("{\"rol\":\"USUARIO\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT ROL FROM USUARIOS WHERE ID = ?", String.class, adminId)).isEqualTo("ADMINISTRADOR");
        assertThat(jdbc.queryForObject("SELECT ACTIVO FROM USUARIOS WHERE ID = ?", Integer.class, adminId)).isEqualTo(1);
    }

    @Test
    void puedeDejarDeSerAdministradorSiQuedaOtro() throws Exception {
        jdbc.update("UPDATE USUARIOS SET ROL = 'ADMINISTRADOR' WHERE ID = ?", usuarioId);
        em.clear();
        mvc.perform(put("/api/usuarios/" + adminId + "/rol").header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON).content("{\"rol\":\"USUARIO\"}"))
                .andExpect(status().isOk());
        em.clear();
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(adminToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("USUARIO"));
    }

    @Test
    void correoDuplicadoAlEditarEs409() throws Exception {
        mvc.perform(put("/api/usuarios/" + usuarioId).header("Authorization", bearer(usuarioToken))
                        .contentType(APPLICATION_JSON).content(datos("Cuenta", " OTRO@EXAMPLE.TEST ")))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT CORREO FROM USUARIOS WHERE ID = ?", String.class, usuarioId))
                .isEqualTo("usuario@example.test");
    }

    @Test
    void cuentaInexistenteEs404YFormatoIdEs400() throws Exception {
        mvc.perform(get("/api/usuarios/99999999").header("Authorization", bearer(adminToken))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/99999999").header("Authorization", bearer(adminToken))).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/abc").header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nombre\":\"Cuenta\",\"correo\":\"usuario@example.test\",\"rol\":\"ADMINISTRADOR\"}",
            "{\"nombre\":\"Cuenta\",\"correo\":\"usuario@example.test\",\"password\":\"        \"}",
            "{\"nombre\":\"Cuenta\",\"correo\":\"usuario@example.test\",\"password\":\"corta\"}"})
    void actualizarDatosInvalidosEs400(String body) throws Exception {
        mvc.perform(put("/api/usuarios/" + usuarioId).header("Authorization", bearer(usuarioToken))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"rol\":1}", "{\"rol\":null}", "{\"rol\":\"OTRO\"}", "{}"})
    void rolInvalidoEs400(String body) throws Exception {
        mvc.perform(put("/api/usuarios/" + usuarioId + "/rol").header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    private long registrar(String correo) throws Exception {
        var response = mvc.perform(post("/api/auth/registro").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nombre", "Cuenta", "correo", correo, "password", "ClavePrueba123"))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString()).get("id").asLong();
    }

    private String login(String correo, String password, int expected) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("correo", correo, "password", password))))
                .andExpect(status().is(expected)).andReturn().getResponse();
        return expected == 200 ? mapper.readTree(response.getContentAsString()).get("accessToken").asText() : "";
    }

    private String datos(String nombre, String correo) throws Exception {
        return mapper.writeValueAsString(Map.of("nombre", nombre, "correo", correo));
    }

    private String hash(long id) {
        return jdbc.queryForObject("SELECT PASSWORD_HASH FROM USUARIOS WHERE ID = ?", String.class, id);
    }

    private String bearer(String token) { return "Bearer " + token; }
}
