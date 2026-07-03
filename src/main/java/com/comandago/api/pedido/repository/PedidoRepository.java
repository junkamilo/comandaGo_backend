package com.comandago.api.pedido.repository;

import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroPedido(String numeroPedido);

    boolean existsByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);

    Page<Pedido> findByEstado(EstadoPedido estado, Pageable pageable);

    Page<Pedido> findByMesaId(Long mesaId, Pageable pageable);

    Page<Pedido> findByOrigen(OrigenPedido origen, Pageable pageable);

    Page<Pedido> findByFechaPedidoBetween(OffsetDateTime desde, OffsetDateTime hasta, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Pedido p "
            + "LEFT JOIN FETCH p.detalles "
            + "LEFT JOIN FETCH p.mesa "
            + "LEFT JOIN FETCH p.usuario "
            + "WHERE p.id = :id")
    Optional<Pedido> findByIdWithDetalles(@Param("id") Long id);

    List<Pedido> findByEstadoInOrderByFechaPedidoAsc(Collection<EstadoPedido> estados);

    List<Pedido> findByMesaIdAndEstadoInOrderByFechaPedidoAsc(Long mesaId, Collection<EstadoPedido> estados);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE pedidos SET impuestos = :impuestos, "
            + "total = subtotal - descuento + :impuestos + costo_envio WHERE id = :id",
            nativeQuery = true)
    void actualizarImpuestosYTotal(@Param("id") Long id, @Param("impuestos") BigDecimal impuestos);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_pedido FROM 10) AS INTEGER)), 0) "
            + "FROM pedidos WHERE numero_pedido LIKE :prefijo || '%'", nativeQuery = true)
    int findMaxSecuenciaDelDia(@Param("prefijo") String prefijo);
}
