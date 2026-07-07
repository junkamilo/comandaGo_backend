package com.comandago.api.producto.repository;

import com.comandago.api.producto.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCategoriaIdAndActivoTrue(Long categoriaId);

    Page<Producto> findByCategoriaId(Long categoriaId, Pageable pageable);

    Page<Producto> findByActivo(Boolean activo, Pageable pageable);

    Page<Producto> findByDisponible(Boolean disponible, Pageable pageable);

    Page<Producto> findByEsPromocion(Boolean esPromocion, Pageable pageable);

    Page<Producto> findByCategoriaIdAndActivoAndDisponible(Long categoriaId, Boolean activo, Boolean disponible, Pageable pageable);

    List<Producto> findByActivoTrueAndDisponibleTrueOrderByOrdenAsc();

    List<Producto> findByActivoTrueAndDisponibleTrueAndCategoriaIdOrderByOrdenAsc(Long categoriaId);

    List<Producto> findByEsPromocionTrueAndActivoTrueAndDisponibleTrueOrderByOrdenAsc();

    @Query("""
            SELECT p FROM Producto p
            JOIN FETCH p.categoria c
            LEFT JOIN FETCH c.categoriaPadre
            WHERE p.activo = true
            ORDER BY c.orden ASC, p.orden ASC
            """)
    List<Producto> findByActivoTrueOrderByCategoriaOrdenAscOrdenAsc();

    @Query("SELECT COALESCE(MAX(p.orden), -1) FROM Producto p WHERE p.categoria.id = :categoriaId")
    int findMaxOrdenEnCategoria(@Param("categoriaId") Long categoriaId);

    long countByActivoTrueAndCategoriaId(Long categoriaId);
}
