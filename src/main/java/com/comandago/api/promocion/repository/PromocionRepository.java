package com.comandago.api.promocion.repository;

import com.comandago.api.promocion.entity.Promocion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    List<Promocion> findAllByOrderByFechaCreacionDesc();

    List<Promocion> findByActivoTrueOrderByFechaCreacionDesc();

    @Query("""
            SELECT DISTINCT p FROM Promocion p
            JOIN FETCH p.productos
            WHERE p.activo = true
              AND p.fechaInicio <= :ahora
              AND (p.fechaFin IS NULL OR p.fechaFin > :ahora)
              AND (p.usoMaximo IS NULL OR p.usoActual < p.usoMaximo)
            ORDER BY p.fechaCreacion DESC
            """)
    List<Promocion> findVigentes(@Param("ahora") OffsetDateTime ahora);

    @Query("""
            SELECT p FROM Promocion p
            JOIN p.productos prod
            WHERE prod.id = :productoId
              AND p.activo = true
              AND p.fechaInicio <= :ahora
              AND (p.fechaFin IS NULL OR p.fechaFin > :ahora)
              AND (p.usoMaximo IS NULL OR p.usoActual < p.usoMaximo)
            ORDER BY p.fechaCreacion DESC
            """)
    List<Promocion> findVigentesParaProducto(
            @Param("productoId") Long productoId,
            @Param("ahora") OffsetDateTime ahora,
            Pageable pageable);

    default Optional<Promocion> findVigenteParaProducto(Long productoId, OffsetDateTime ahora) {
        return findVigentesParaProducto(productoId, ahora, Pageable.ofSize(1)).stream().findFirst();
    }
}
