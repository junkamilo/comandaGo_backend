package com.comandago.api.mesa.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MesaAgruparRequest {

    @NotEmpty(message = "Debe indicar al menos una mesa")
    @Size(min = 2, message = "Se necesitan al menos 2 mesas para agrupar")
    private List<@Positive(message = "El id de mesa debe ser positivo") Long> mesaIds;
}
