package cl.duoc.forousuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ForoUsuariosApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForoUsuariosApplication.class, args);
    }
}
