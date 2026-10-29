package cl.duoc.forousuarios.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> credencialesInvalidas() {
        ProblemDetail error = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        error.setTitle("Autenticación requerida");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("WWW-Authenticate", "Bearer").body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail error = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise los campos enviados");
        error.setTitle("Datos inválidos");
        error.setProperty("errors", campos);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> jsonInvalido() {
        ProblemDetail error = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Revise el JSON, sus tipos de datos y los campos permitidos");
        error.setTitle("Solicitud inválida");
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({CorreoDuplicadoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ProblemDetail> conflicto(Exception ex) {
        String detail = ex instanceof CorreoDuplicadoException
                ? "El correo ya está registrado"
                : "No se pudo completar la operación por un conflicto de datos";
        ProblemDetail error = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        error.setTitle("Conflicto de datos");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
