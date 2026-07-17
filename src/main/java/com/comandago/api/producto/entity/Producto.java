package com.comandago.api.producto.entity;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.receta.entity.Receta;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_promocion", precision = 12, scale = 2)
    private BigDecimal precioPromocion;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "es_promocion", nullable = false)
    @Builder.Default
    private Boolean esPromocion = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disponible = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoProducto tipo = TipoProducto.NORMAL;

    @OneToMany(mappedBy = "productoCompuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<ProductoInsumo> composicion = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id")
    private Receta receta;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    public BigDecimal getPrecioFinal() {
        if (Boolean.TRUE.equals(esPromocion) && precioPromocion != null) {
            return precioPromocion;
        }
        return precio;
    }
}
