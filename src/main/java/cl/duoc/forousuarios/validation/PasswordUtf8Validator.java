package cl.duoc.forousuarios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordUtf8Validator implements ConstraintValidator<PasswordUtf8, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotBlank informa el valor ausente.
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        return bytes >= 8 && bytes <= 72;
    }
}
