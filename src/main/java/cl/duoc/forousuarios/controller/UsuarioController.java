package cl.duoc.forousuarios.controller;

import cl.duoc.forousuarios.dto.ActualizarRolRequest;
import cl.duoc.forousuarios.dto.ActualizarUsuarioRequest;
import cl.duoc.forousuarios.dto.UsuarioResponse;
import cl.duoc.forousuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping
    public List<UsuarioResponse> listar(@AuthenticationPrincipal UsuarioResponse actor) {
        return usuarios.listar(actor);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioResponse actor) {
        return usuarios.obtener(id, actor);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest datos,
            @AuthenticationPrincipal UsuarioResponse actor) {
        return usuarios.actualizar(id, datos, actor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioResponse actor) {
        usuarios.desactivar(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody ActualizarRolRequest datos,
            @AuthenticationPrincipal UsuarioResponse actor) {
        return usuarios.cambiarRol(id, datos.rol(), actor);
    }
}
