package com.comandago.api.pago.repository;

import com.comandago.api.pago.entity.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Page<Pago> findByPedidoId(Long pedidoId, Pageable pageable);

    List<Pago> findByPedidoIdOrderByFechaPagoAsc(Long pedidoId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.pedido.id = :pedidoId")
    BigDecimal sumMontoByPedidoId(@Param("pedidoId") Long pedidoId);
}
