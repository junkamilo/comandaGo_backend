package com.comandago.api.receta.repository;

import com.comandago.api.receta.entity.RecetaIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaIngredienteRepository extends JpaRepository<RecetaIngrediente, Long> {

    List<RecetaIngrediente> findByRecetaIdOrderByOrdenAsc(Long recetaId);

    List<RecetaIngrediente> findByProductoId(Long productoId);
}
