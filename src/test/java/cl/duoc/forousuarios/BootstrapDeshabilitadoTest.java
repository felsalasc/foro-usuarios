package cl.duoc.forousuarios;

import cl.duoc.forousuarios.bootstrap.AdministradorInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"local", "test"})
class BootstrapDeshabilitadoTest {
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    @Test
    void arranqueNormalNoCreaAdministrador() {
        assertThat(context.getBeansOfType(AdministradorInitializer.class)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS", Long.class)).isZero();
    }
}
