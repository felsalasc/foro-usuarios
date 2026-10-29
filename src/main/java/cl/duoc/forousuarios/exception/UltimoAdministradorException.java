package cl.duoc.forousuarios.exception;

public class UltimoAdministradorException extends RuntimeException {
    public UltimoAdministradorException() { super("Debe permanecer al menos un administrador activo"); }
}
