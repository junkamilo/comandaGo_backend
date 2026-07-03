package com.comandago.api.producto.dto.validation;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

@Documented
@Constraint(validatedBy = PromocionValidator.Validator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PromocionValidator {

    String message() default "Si el producto está en promoción, el precio de promoción es obligatorio";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PromocionValidator, ProductoCreateRequest> {

        @Override
        public boolean isValid(ProductoCreateRequest request, ConstraintValidatorContext context) {
            if (!Boolean.TRUE.equals(request.getEsPromocion())) {
                return true;
            }
            BigDecimal precioPromocion = request.getPrecioPromocion();
            if (precioPromocion == null) {
                return false;
            }
            BigDecimal precio = request.getPrecio();
            return precio == null || precioPromocion.compareTo(precio) < 0;
        }
    }
}
