package com.comandago.api.producto.entity;

import com.comandago.api.producto.enums.UnidadInsumo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "producto_insumos")
@Getter
@Setter
@NoArgsConstructor
public class ProductoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_compuesto_id", nullable = false)
    private Producto productoCompuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_insumo_id", nullable = false)
    private Producto productoInsumo;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadInsumo unidad = UnidadInsumo.UND;

    @Column(name = "es_removible", nullable = false)
    private Boolean esRemovible = true;

    @Column(name = "es_extra", nullable = false)
    private Boolean esExtra = false;

    @Column(name = "precio_extra", precision = 12, scale = 2)
    private BigDecimal precioExtra;

    @Column(nullable = false)
    private Integer orden = 0;
}
