package com.comandago.api.receta.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas")
@Getter
@Setter
@NoArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String preparacion;

    @Column(name = "tiempo_total_min")
    private Integer tiempoTotalMin;

    private Integer porciones;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<RecetaIngrediente> ingredientes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private OffsetDateTime fechaActualizacion;

    public void agregarIngrediente(RecetaIngrediente ingrediente) {
        ingredientes.add(ingrediente);
        ingrediente.setReceta(this);
    }

    public void limpiarIngredientes() {
        ingredientes.clear();
    }
}
