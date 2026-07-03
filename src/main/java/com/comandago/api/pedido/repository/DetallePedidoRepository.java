package com.comandago.api.pedido.repository;

import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    List<DetallePedido> findByPedidoId(Long pedidoId);

    Optional<DetallePedido> findByIdAndPedidoId(Long id, Long pedidoId);

    @Query("SELECT d FROM DetallePedido d JOIN FETCH d.producto WHERE d.pedido.id = :pedidoId")
    List<DetallePedido> findByPedidoIdWithProducto(@Param("pedidoId") Long pedidoId);

    List<DetallePedido> findByEstadoIn(List<EstadoDetalle> estados);
}
