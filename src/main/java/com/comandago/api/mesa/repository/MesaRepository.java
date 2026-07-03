package com.comandago.api.mesa.repository;

import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    boolean existsByNumeroIgnoreCase(String numero);

    boolean existsByNumeroIgnoreCaseAndIdNot(String numero, Long id);

    boolean existsByQrToken(String qrToken);

    Optional<Mesa> findByQrToken(String qrToken);

    Page<Mesa> findByEstado(EstadoMesa estado, Pageable pageable);

    Page<Mesa> findByActivo(Boolean activo, Pageable pageable);

    Page<Mesa> findByEstadoAndActivo(EstadoMesa estado, Boolean activo, Pageable pageable);
}
