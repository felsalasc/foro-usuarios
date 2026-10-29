package cl.duoc.forousuarios.dto;

import cl.duoc.forousuarios.model.Rol;

public record UsuarioResponse(Long id, String nombre, String correo, Rol rol) {
}
