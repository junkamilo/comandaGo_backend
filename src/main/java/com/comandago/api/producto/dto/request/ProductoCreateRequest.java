package com.comandago.api.producto.dto.request;

import com.comandago.api.producto.dto.validation.CategoriaSegunTipoCreateValidator;
import com.comandago.api.producto.dto.validation.PromocionValidator;
import com.comandago.api.producto.enums.TipoProducto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@PromocionValidator
@CategoriaSegunTipoCreateValidator
public class ProductoCreateRequest {

    /** Null permitido solo para INSUMO (uso interno / solo receta). */
    private Long categoriaId;

    @NotNull(message = "El tipo es obligatorio")
    private TipoProducto tipo = TipoProducto.NORMAL;

    @Valid
    private List<ProductoInsumoRequest> composicion;

    /** Receta reutilizable; obligatoria para COMPUESTO. */
    private Long recetaId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio tiene formato inválido")
    private BigDecimal precio;

    @DecimalMin(value = "0.00", message = "El precio de promoción debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio de promoción tiene formato inválido")
    private BigDecimal precioPromocion;

    @Size(max = 255, message = "La URL de imagen no puede superar 255 caracteres")
    @URL(message = "La URL de imagen no es válida")
    private String imagenUrl;

    private Boolean esPromocion;
}
