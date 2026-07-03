package com.comandago.api.mesa.dto.mapper;

import com.comandago.api.mesa.dto.request.MesaCreateRequest;
import com.comandago.api.mesa.dto.request.MesaUpdateRequest;
import com.comandago.api.mesa.dto.response.MesaResponse;
import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import org.springframework.stereotype.Component;

@Component
public class MesaMapper {

    public Mesa toEntity(MesaCreateRequest request, String qrToken) {
        return Mesa.builder()
                .numero(request.getNumero())
                .nombre(request.getNombre())
                .capacidad(request.getCapacidad())
                .qrToken(qrToken)
                .estado(request.getEstado() != null ? request.getEstado() : EstadoMesa.LIBRE)
                .activo(true)
                .build();
    }

    public void updateEntity(Mesa mesa, MesaUpdateRequest request) {
        if (request.getNumero() != null) {
            mesa.setNumero(request.getNumero());
        }
        if (request.getNombre() != null) {
            mesa.setNombre(request.getNombre());
        }
        if (request.getCapacidad() != null) {
            mesa.setCapacidad(request.getCapacidad());
        }
        if (request.getQrToken() != null) {
            mesa.setQrToken(request.getQrToken());
        }
        if (request.getEstado() != null) {
            mesa.setEstado(request.getEstado());
        }
    }

    public MesaResponse toResponse(Mesa mesa) {
        return MesaResponse.builder()
                .id(mesa.getId())
                .numero(mesa.getNumero())
                .nombre(mesa.getNombre())
                .capacidad(mesa.getCapacidad())
                .qrToken(mesa.getQrToken())
                .estado(mesa.getEstado())
                .activo(mesa.getActivo())
                .build();
    }
}
