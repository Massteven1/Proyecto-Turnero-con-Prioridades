package com.banco.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

// Verifica que el texto no supere el maximo de bytes al codificarse en UTF-8.
// Null lo deja pasar: ese caso lo maneja @NotBlank.
public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int maximo;

    @Override
    public void initialize(MaxUtf8Bytes anotacion) {
        this.maximo = anotacion.value();
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        return valor.getBytes(StandardCharsets.UTF_8).length <= maximo;
    }
}
