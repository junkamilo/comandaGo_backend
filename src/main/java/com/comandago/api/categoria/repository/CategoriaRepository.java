package com.comandago.api.categoria.repository;

import com.comandago.api.categoria.entity.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);

    boolean existsByNombreIgnoreCaseAndActivoTrueAndIdNot(String nombre, Long id);

    Page<Categoria> findByActivo(Boolean activo, Pageable pageable);

    Page<Categoria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Categoria> findByActivoAndNombreContainingIgnoreCase(Boolean activo, String nombre, Pageable pageable);

    List<Categoria> findByActivoTrueAndCategoriaPadreIsNullOrderByOrdenAsc();

    List<Categoria> findByActivoTrueAndCategoriaPadreIdOrderByOrdenAsc(Long padreId);

    List<Categoria> findAllByOrderByOrdenAsc();

    boolean existsByCategoriaPadreIdAndActivoTrue(Long padreId);

    boolean existsByCategoriaPadreId(Long padreId);
}
