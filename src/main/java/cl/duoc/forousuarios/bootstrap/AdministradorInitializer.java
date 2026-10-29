package cl.duoc.forousuarios.bootstrap;

import cl.duoc.forousuarios.dto.RegistroRequest;
import cl.duoc.forousuarios.service.UsuarioService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class AdministradorInitializer implements ApplicationRunner {
    private final UsuarioService usuarios;
    private final Validator validator;
    private final RegistroRequest datos;

    public AdministradorInitializer(UsuarioService usuarios, Validator validator,
            @Value("${app.bootstrap.nombre}") String nombre,
            @Value("${app.bootstrap.correo}") String correo,
            @Value("${app.bootstrap.password}") String password) {
        this.usuarios = usuarios;
        this.validator = validator;
        this.datos = new RegistroRequest(nombre, correo, password);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!validator.validate(datos).isEmpty()) {
            throw new IllegalStateException("Configuración del administrador inicial inválida: revise nombre, correo y contraseña");
        }
        usuarios.crearAdministradorInicial(datos);
    }
}
