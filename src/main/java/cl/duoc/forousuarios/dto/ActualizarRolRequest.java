package cl.duoc.forousuarios.dto;

import cl.duoc.forousuarios.model.Rol;
import jakarta.validation.constraints.NotNull;

public record ActualizarRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {
}
