/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.models.saga;

/**
 *
 * @author eiler
 */
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_pasos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class EntidadSagaPaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(nullable = false, length = 40)
    private String paso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPaso tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPaso estado;

    @Column(length = 500)
    private String detalle;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;
}