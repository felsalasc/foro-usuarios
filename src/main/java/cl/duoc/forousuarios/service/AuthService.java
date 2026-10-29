package cl.duoc.forousuarios.service;

import cl.duoc.forousuarios.dto.LoginRequest;
import cl.duoc.forousuarios.dto.LoginResponse;
import cl.duoc.forousuarios.dto.UsuarioResponse;
import cl.duoc.forousuarios.model.Usuario;
import cl.duoc.forousuarios.repository.UsuarioRepository;
import cl.duoc.forousuarios.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwords;
    private final JwtService tokens;
    private final String dummyHash;

    public AuthService(UsuarioRepository repository, PasswordEncoder passwords, JwtService tokens) {
        this.repository = repository;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.encode("Cuenta inexistente - solo comparacion");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest datos) {
        var encontrado = repository.findByCorreo(datos.correo());
        String hash = encontrado.map(Usuario::getPasswordHash).orElse(dummyHash);
        boolean longitudValida = datos.password().getBytes(StandardCharsets.UTF_8).length <= 72;
        boolean coincide = passwords.matches(longitudValida ? datos.password() : "", hash);
        if (encontrado.isEmpty() || !coincide || !longitudValida || !Boolean.TRUE.equals(encontrado.get().getActivo())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        Usuario usuario = encontrado.get();
        return new LoginResponse(tokens.emitir(usuario.getId()), "Bearer", 900, respuesta(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse identidad(Long id) {
        Usuario usuario = repository.findById(id).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        return respuesta(usuario);
    }

    private UsuarioResponse respuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
    }
}
