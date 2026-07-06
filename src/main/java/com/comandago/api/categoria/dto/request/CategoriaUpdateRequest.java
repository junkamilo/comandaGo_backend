package com.comandago.api.categoria.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaUpdateRequest {

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    @Size(max = 255, message = "La URL de imagen no puede superar 255 caracteres")
    private String imagenUrl;

    private Long categoriaPadreId;
}
