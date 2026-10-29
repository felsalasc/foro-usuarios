package cl.duoc.forousuarios;

import cl.duoc.forousuarios.bootstrap.AdministradorInitializer;
import cl.duoc.forousuarios.dto.RegistroRequest;
import cl.duoc.forousuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"app.bootstrap.enabled=true", "app.bootstrap.nombre=Administrador",
        "app.bootstrap.correo=bootstrap@example.test", "app.bootstrap.password=ClaveBootstrap123",
        "spring.datasource.url=jdbc:h2:mem:bootstrap_test;MODE=Oracle;DB_CLOSE_DELAY=-1"})
@ActiveProfiles({"local", "test"})
class AdministradorInitializerTest {
    @Autowired AdministradorInitializer initializer;
    @Autowired UsuarioService usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired Validator validator;

    @Test
    void creaAdministradorYRepetirNoDuplicaNiCambiaPassword() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS", Long.class)).isEqualTo(1);
        String hash = jdbc.queryForObject("SELECT PASSWORD_HASH FROM USUARIOS", String.class);
        assertThat(new BCryptPasswordEncoder().matches("ClaveBootstrap123", hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT ROL FROM USUARIOS", String.class)).isEqualTo("ADMINISTRADOR");
        initializer.run(new DefaultApplicationArguments());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT PASSWORD_HASH FROM USUARIOS", String.class)).isEqualTo(hash);
    }

    @Test
    @Transactional
    void noPromueveCuentaExistenteNiReemplazaSuPassword() {
        jdbc.update("UPDATE USUARIOS SET ROL = 'USUARIO'");
        assertThatThrownBy(() -> usuarios.crearAdministradorInicial(
                new RegistroRequest("Otro", "bootstrap@example.test", "NuevaClave123")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT ROL FROM USUARIOS", String.class)).isEqualTo("USUARIO");
        assertThat(new BCryptPasswordEncoder().matches("ClaveBootstrap123",
                jdbc.queryForObject("SELECT PASSWORD_HASH FROM USUARIOS", String.class))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "correo", "password"})
    void credencialesInvalidasImpidenInicializacion(String campo) {
        var runner = new AdministradorInitializer(usuarios, validator,
                campo.equals("nombre") ? "" : "Administrador",
                campo.equals("correo") ? "correo-invalido" : "nuevo@example.test",
                campo.equals("password") ? "corta" : "ClaveBootstrap123");
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS", Long.class)).isEqualTo(1);
    }
}
