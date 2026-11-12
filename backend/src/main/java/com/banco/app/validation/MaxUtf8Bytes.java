package com.banco.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Exigida por el contrato de endpoints: la contrasena no debe exceder 72 bytes
// en UTF-8 (BCrypt solo considera los primeros 72 bytes y trunca el resto).
// La longitud en CARACTERES la valida @Size; esta anotacion valida BYTES,
// que es distinto cuando hay tildes, enes u otros caracteres multibyte.
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value() default 72;

    String message() default "no debe exceder {value} bytes en UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
