/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.models.shipping;

/**
 *
 * @author eiler
 */
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "envios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class EntidadEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(nullable = false, length = 120)
    private String cliente;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(nullable = false, length = 80)
    private String guia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;
}
