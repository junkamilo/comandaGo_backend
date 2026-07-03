package com.comandago.api.categoria.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class CategoriaUpdateRequest {

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    @Size(max = 255, message = "La URL de imagen no puede superar 255 caracteres")
    @URL(message = "La URL de imagen no es válida")
    private String imagenUrl;

    @Min(value = 0, message = "El orden debe ser mayor o igual a 0")
    private Integer orden;
}
