package cl.duoc.forousuarios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = PasswordUtf8Validator.class)
public @interface PasswordUtf8 {
    String message() default "La contraseña debe tener entre 8 y 72 bytes UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
