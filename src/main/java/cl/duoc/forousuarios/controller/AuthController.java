package cl.duoc.forousuarios.controller;

import cl.duoc.forousuarios.dto.RegistroRequest;
import cl.duoc.forousuarios.dto.LoginRequest;
import cl.duoc.forousuarios.dto.LoginResponse;
import cl.duoc.forousuarios.dto.UsuarioResponse;
import cl.duoc.forousuarios.service.AuthService;
import cl.duoc.forousuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService usuarios;
    private final AuthService auth;

    public AuthController(UsuarioService usuarios, AuthService auth) {
        this.usuarios = usuarios;
        this.auth = auth;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        UsuarioResponse creado = usuarios.registrar(datos);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/usuarios/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        return auth.login(datos);
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioResponse identidad) {
        return identidad;
    }
}
