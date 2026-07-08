package com.comandago.api.pago.repository;

import com.comandago.api.pago.enums.EstadoTransaccionPago;
import com.comandago.api.pago.enums.MetodoPago;
import com.comandago.api.pago.entity.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Page<Pago> findByPedidoId(Long pedidoId, Pageable pageable);

    List<Pago> findByPedidoIdOrderByFechaPagoAsc(Long pedidoId);

    @Query("""
            SELECT COALESCE(SUM(p.monto), 0)
            FROM Pago p
            WHERE p.pedido.id = :pedidoId
              AND p.estado = com.comandago.api.pago.enums.EstadoTransaccionPago.COMPLETADO
            """)
    BigDecimal totalPagadoPorPedido(@Param("pedidoId") Long pedidoId);

    @Query("""
            SELECT COALESCE(SUM(p.propina), 0)
            FROM Pago p
            WHERE p.pedido.id = :pedidoId
              AND p.estado = com.comandago.api.pago.enums.EstadoTransaccionPago.COMPLETADO
            """)
    BigDecimal totalPropinasPorPedido(@Param("pedidoId") Long pedidoId);

    List<Pago> findByEstadoAndFechaPagoBetweenOrderByFechaPagoAsc(
            EstadoTransaccionPago estado, OffsetDateTime desde, OffsetDateTime hasta);

    @Query("""
            SELECT COALESCE(SUM(p.monto), 0)
            FROM Pago p
            WHERE p.metodo = :metodo
              AND p.estado = com.comandago.api.pago.enums.EstadoTransaccionPago.COMPLETADO
              AND p.fechaPago BETWEEN :desde AND :hasta
            """)
    BigDecimal totalPorMetodoEnRango(
            @Param("metodo") MetodoPago metodo,
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);

    @Query("""
            SELECT COALESCE(SUM(p.propina), 0)
            FROM Pago p
            WHERE p.estado = com.comandago.api.pago.enums.EstadoTransaccionPago.COMPLETADO
              AND p.fechaPago BETWEEN :desde AND :hasta
            """)
    BigDecimal totalPropinasEnRango(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);
}
