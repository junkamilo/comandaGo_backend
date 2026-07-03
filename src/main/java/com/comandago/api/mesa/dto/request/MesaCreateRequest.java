package com.comandago.api.mesa.dto.request;

import com.comandago.api.mesa.enums.EstadoMesa;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MesaCreateRequest {

    @NotBlank(message = "El número de mesa es obligatorio")
    @Size(max = 20, message = "El número no puede superar 20 caracteres")
    private String numero;

    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    private String nombre;

    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    private Integer capacidad;

    @Size(max = 100, message = "El token QR no puede superar 100 caracteres")
    private String qrToken;

    private EstadoMesa estado;
}
