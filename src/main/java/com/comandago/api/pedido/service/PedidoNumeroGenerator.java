package com.comandago.api.pedido.service;

import com.comandago.api.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class PedidoNumeroGenerator {

    private static final ZoneId ZONA_CO = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PedidoRepository pedidoRepository;

    @Transactional
    public synchronized String generar() {
        String prefijo = LocalDate.now(ZONA_CO).format(FORMATTER) + "-";
        int secuencia = pedidoRepository.findMaxSecuenciaDelDia(prefijo) + 1;
        return prefijo + String.format("%04d", secuencia);
    }
}
