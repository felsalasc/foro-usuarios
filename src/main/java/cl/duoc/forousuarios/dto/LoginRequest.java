package cl.duoc.forousuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres") String correo,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
    public LoginRequest {
        correo = correo == null ? null : correo.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "LoginRequest[datos protegidos]";
    }
}
