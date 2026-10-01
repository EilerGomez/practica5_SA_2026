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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sagas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class EntidadSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, unique = true, length = 60)
    private String sagaId;

    @Column(nullable = false, length = 120)
    private String cliente;

    @Column(nullable = false, length = 120)
    private String producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSaga estado;

    @Column(name = "motivo_fallo", length = 500)
    private String motivoFallo;

    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "pago_id")
    private Long pagoId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "envio_id")
    private Long envioId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;
}
