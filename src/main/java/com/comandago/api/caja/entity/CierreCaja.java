package com.comandago.api.caja.entity;

import com.comandago.api.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cierres_caja")
@Getter
@Setter
@NoArgsConstructor
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private OffsetDateTime fechaApertura;

    @Column(name = "fecha_cierre", nullable = false)
    private OffsetDateTime fechaCierre;

    @Column(name = "total_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_tarjeta", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(name = "total_nequi", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNequi = BigDecimal.ZERO;

    @Column(name = "total_daviplata", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDaviplata = BigDecimal.ZERO;

    @Column(name = "total_transferencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTransferencia = BigDecimal.ZERO;

    @Column(name = "total_otros", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOtros = BigDecimal.ZERO;

    @Column(name = "total_propinas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPropinas = BigDecimal.ZERO;

    @Column(name = "total_general", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "pedidos_atendidos", nullable = false)
    private Integer pedidosAtendidos = 0;

    @Column(name = "pedidos_cancelados", nullable = false)
    private Integer pedidosCancelados = 0;

    @Column(columnDefinition = "TEXT")
    private String notas;
}
