package cl.duoc.forousuarios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiSecurityErrorWriter {
    private final ObjectMapper mapper;

    public ApiSecurityErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escribir(HttpServletResponse response, HttpStatus status) throws IOException {
        boolean unauthorized = status == HttpStatus.UNAUTHORIZED;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                unauthorized ? "Credenciales inválidas o autenticación requerida" : "Permisos insuficientes");
        problem.setTitle(unauthorized ? "Autenticación requerida" : "Acceso denegado");
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        if (unauthorized) response.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
