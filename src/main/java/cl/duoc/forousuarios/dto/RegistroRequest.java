package cl.duoc.forousuarios.dto;

import cl.duoc.forousuarios.validation.PasswordUtf8;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres") String nombre,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres") String correo,
        @NotBlank(message = "La contraseña es obligatoria") @PasswordUtf8 String password) {

    public RegistroRequest {
        nombre = nombre == null ? null : nombre.strip();
        correo = correo == null ? null : correo.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegistroRequest[datos protegidos]";
    }
}
