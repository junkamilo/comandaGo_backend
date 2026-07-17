package com.comandago.api.receta.repository;

import com.comandago.api.receta.entity.Receta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Receta> findByActivoTrueOrderByNombreAsc();

    List<Receta> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"ingredientes", "ingredientes.producto", "ingredientes.producto.categoria"})
    @Query("SELECT r FROM Receta r WHERE r.id = :id")
    Optional<Receta> findByIdWithIngredientes(@Param("id") Long id);

    @EntityGraph(attributePaths = {"ingredientes", "ingredientes.producto", "ingredientes.producto.categoria"})
    @Query("SELECT r FROM Receta r ORDER BY r.nombre ASC")
    List<Receta> findAllWithIngredientesOrderByNombreAsc();

    @EntityGraph(attributePaths = {"ingredientes", "ingredientes.producto", "ingredientes.producto.categoria"})
    @Query("SELECT r FROM Receta r WHERE r.activo = true ORDER BY r.nombre ASC")
    List<Receta> findActivasWithIngredientesOrderByNombreAsc();
}
