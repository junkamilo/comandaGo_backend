package com.comandago.api.producto.dto.validation;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.enums.TipoProducto;
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
@Constraint(validatedBy = CategoriaSegunTipoCreateValidator.Validator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoriaSegunTipoCreateValidator {

    String message() default "Los productos NORMAL/COMPUESTO requieren categoría";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CategoriaSegunTipoCreateValidator, ProductoCreateRequest> {

        @Override
        public boolean isValid(ProductoCreateRequest request, ConstraintValidatorContext context) {
            if (request == null) {
                return true;
            }
            TipoProducto tipo = request.getTipo() != null ? request.getTipo() : TipoProducto.NORMAL;
            if (tipo == TipoProducto.INSUMO) {
                return true;
            }
            if (request.getCategoriaId() != null) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("La categoría es obligatoria para este tipo de producto")
                    .addPropertyNode("categoriaId")
                    .addConstraintViolation();
            return false;
        }
    }
}
