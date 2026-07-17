package com.comandago.api.receta.entity;

import com.comandago.api.producto.entity.Producto;
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
@Table(name = "receta_ingredientes")
@Getter
@Setter
@NoArgsConstructor
public class RecetaIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadInsumo unidad = UnidadInsumo.UND;

    @Column(name = "es_removible", nullable = false)
    private Boolean esRemovible = false;

    @Column(nullable = false)
    private Integer orden = 0;
}
