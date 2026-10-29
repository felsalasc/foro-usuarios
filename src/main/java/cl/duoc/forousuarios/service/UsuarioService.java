package cl.duoc.forousuarios.service;

import cl.duoc.forousuarios.dto.RegistroRequest;
import cl.duoc.forousuarios.dto.ActualizarUsuarioRequest;
import cl.duoc.forousuarios.dto.UsuarioResponse;
import cl.duoc.forousuarios.exception.CorreoDuplicadoException;
import cl.duoc.forousuarios.exception.UsuarioNoEncontradoException;
import cl.duoc.forousuarios.exception.UltimoAdministradorException;
import cl.duoc.forousuarios.model.Rol;
import cl.duoc.forousuarios.model.Usuario;
import cl.duoc.forousuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager em;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, EntityManager em) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.em = em;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        if (repository.existsByCorreo(datos.correo())) throw new CorreoDuplicadoException();
        Usuario usuario = repository.saveAndFlush(new Usuario(datos.nombre(), datos.correo(),
                passwordEncoder.encode(datos.password())));
        return respuesta(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(UsuarioResponse actor) {
        exigirAdministrador(actor);
        return repository.findByActivoTrueOrderByIdAsc().stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id, UsuarioResponse actor) {
        exigirAcceso(id, actor);
        return respuesta(repository.findById(id).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(UsuarioNoEncontradoException::new));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest datos, UsuarioResponse actor) {
        exigirAcceso(id, actor);
        Usuario usuario = activoBloqueado(id);
        if (!id.equals(actor.id())) exigirAdministradorVigente(actor);
        var duplicado = repository.findByCorreo(datos.correo());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(id)) throw new CorreoDuplicadoException();
        String hash = datos.password() == null ? null : passwordEncoder.encode(datos.password());
        usuario.actualizar(datos.nombre(), datos.correo(), hash);
        return respuesta(repository.saveAndFlush(usuario));
    }

    @Transactional
    public void desactivar(Long id, UsuarioResponse actor) {
        exigirAcceso(id, actor);
        repository.bloquearActivosPorRol(Rol.ADMINISTRADOR);
        if (!id.equals(actor.id())) exigirAdministradorVigente(actor);
        Usuario usuario = activoBloqueado(id);
        protegerUltimoAdministrador(usuario, true);
        usuario.desactivar();
        repository.saveAndFlush(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, Rol rol, UsuarioResponse actor) {
        exigirAdministrador(actor);
        repository.bloquearActivosPorRol(Rol.ADMINISTRADOR);
        exigirAdministradorVigente(actor);
        Usuario usuario = activoBloqueado(id);
        protegerUltimoAdministrador(usuario, rol != Rol.ADMINISTRADOR);
        usuario.cambiarRol(rol);
        return respuesta(repository.saveAndFlush(usuario));
    }

    @Transactional
    public void crearAdministradorInicial(RegistroRequest datos) {
        if (!repository.bloquearActivosPorRol(Rol.ADMINISTRADOR).isEmpty()) return;
        if (repository.existsByCorreo(datos.correo())) {
            throw new IllegalStateException("El correo de inicialización ya pertenece a una cuenta; no se cambian sus privilegios");
        }
        Usuario usuario = new Usuario(datos.nombre(), datos.correo(), passwordEncoder.encode(datos.password()));
        usuario.cambiarRol(Rol.ADMINISTRADOR);
        repository.saveAndFlush(usuario);
    }

    private Usuario activoBloqueado(Long id) {
        Usuario usuario = repository.bloquearPorId(id).orElseThrow(UsuarioNoEncontradoException::new);
        // Volver a leer bajo el bloqueo evita reutilizar estado anterior del contexto JPA.
        em.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
        if (!Boolean.TRUE.equals(usuario.getActivo())) throw new UsuarioNoEncontradoException();
        return usuario;
    }

    private void protegerUltimoAdministrador(Usuario usuario, boolean quitaPrivilegio) {
        if (quitaPrivilegio && usuario.getRol() == Rol.ADMINISTRADOR
                && repository.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw new UltimoAdministradorException();
        }
    }

    private void exigirAcceso(Long id, UsuarioResponse actor) {
        if (!id.equals(actor.id())) exigirAdministrador(actor);
    }

    private void exigirAdministrador(UsuarioResponse actor) {
        if (actor.rol() != Rol.ADMINISTRADOR) throw new AccessDeniedException("Permisos insuficientes");
    }

    private void exigirAdministradorVigente(UsuarioResponse actor) {
        if (!repository.existsByIdAndRolAndActivoTrue(actor.id(), Rol.ADMINISTRADOR)) {
            throw new AccessDeniedException("Permisos insuficientes");
        }
    }

    private UsuarioResponse respuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
    }
}
