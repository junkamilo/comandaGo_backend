package com.comandago.api.producto.repository;

import com.comandago.api.producto.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCategoriaIdAndActivoTrue(Long categoriaId);

    Page<Producto> findByCategoriaId(Long categoriaId, Pageable pageable);

    Page<Producto> findByActivo(Boolean activo, Pageable pageable);

    Page<Producto> findByDisponible(Boolean disponible, Pageable pageable);

    Page<Producto> findByEsPromocion(Boolean esPromocion, Pageable pageable);

    Page<Producto> findByCategoriaIdAndActivoAndDisponible(Long categoriaId, Boolean activo, Boolean disponible, Pageable pageable);

    List<Producto> findByActivoTrueAndDisponibleTrueOrderByOrdenAsc();
}
