package cl.duoc.forousuarios.service;

import cl.duoc.forousuarios.dto.RegistroRequest;
import cl.duoc.forousuarios.dto.UsuarioResponse;
import cl.duoc.forousuarios.exception.CorreoDuplicadoException;
import cl.duoc.forousuarios.model.Usuario;
import cl.duoc.forousuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        if (repository.existsByCorreo(datos.correo())) throw new CorreoDuplicadoException();
        Usuario usuario = repository.saveAndFlush(new Usuario(datos.nombre(), datos.correo(),
                passwordEncoder.encode(datos.password())));
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
    }
}
