package com.comandago.api.producto.repository;

import com.comandago.api.producto.entity.ProductoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoInsumoRepository extends JpaRepository<ProductoInsumo, Long> {

    List<ProductoInsumo> findByProductoCompuestoIdOrderByOrdenAsc(Long compuestoId);

    List<ProductoInsumo> findByProductoInsumoId(Long insumoId);

    void deleteByProductoCompuestoId(Long compuestoId);
}
