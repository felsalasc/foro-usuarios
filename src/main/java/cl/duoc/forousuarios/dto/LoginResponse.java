package cl.duoc.forousuarios.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) {
    @Override
    public String toString() {
        return "LoginResponse[token protegido]";
    }
}
