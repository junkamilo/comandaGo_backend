package com.comandago.api.caja.repository;

import com.comandago.api.caja.entity.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    List<CierreCaja> findAllByOrderByFechaCierreDesc();

    Optional<CierreCaja> findTopByOrderByFechaCierreDesc();

    @Query("""
            SELECT COUNT(c) > 0 FROM CierreCaja c
            WHERE c.fechaCierre > :desde
            """)
    boolean existsCierreDespuesDe(@Param("desde") OffsetDateTime desde);

    List<CierreCaja> findByFechaCierreBetweenOrderByFechaCierreDesc(
            OffsetDateTime desde, OffsetDateTime hasta);
}
