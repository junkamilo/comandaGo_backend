package com.comandago.api.mesa.entity;

import com.comandago.api.mesa.enums.EstadoMesa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mesas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(length = 50)
    private String nombre;

    private Integer capacidad;

    @Column(name = "qr_token", unique = true, length = 100)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoMesa estado = EstadoMesa.LIBRE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "grupo_id", length = 36)
    private String grupoId;
}
