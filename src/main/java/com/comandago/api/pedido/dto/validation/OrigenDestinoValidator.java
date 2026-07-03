package com.comandago.api.pedido.dto.validation;

import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.enums.OrigenPedido;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = OrigenDestinoValidator.Validator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OrigenDestinoValidator {

    String message() default "Los datos del pedido no son coherentes con el origen";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<OrigenDestinoValidator, PedidoCreateRequest> {

        @Override
        public boolean isValid(PedidoCreateRequest request, ConstraintValidatorContext context) {
            if (request.getOrigen() == null) {
                return true;
            }
            return switch (request.getOrigen()) {
                case MESA_MESERO, MESA_QR -> request.getMesaId() != null;
                case WEB_DOMICILIO -> request.getDireccionEntrega() != null
                        && !request.getDireccionEntrega().isBlank();
            };
        }
    }
}
