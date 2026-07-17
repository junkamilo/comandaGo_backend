package com.comandago.api.receta.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecetaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
    private String descripcion;

    @Size(max = 20000, message = "La preparación no puede superar 20000 caracteres")
    private String preparacion;

    @PositiveOrZero(message = "El tiempo no puede ser negativo")
    private Integer tiempoTotalMin;

    @Positive(message = "Las porciones deben ser mayores a 0")
    private Integer porciones;

    private Boolean activo;

    @NotEmpty(message = "La receta debe tener al menos un ingrediente")
    @Valid
    private List<IngredienteRequest> ingredientes;
}
