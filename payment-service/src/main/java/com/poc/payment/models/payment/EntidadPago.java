/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.models.payment;

/**
 *
 * @author eiler
 */

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class EntidadPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(nullable = false, length = 120)
    private String cliente;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false, length = 80)
    private String referencia;

    @Column(name = "fecha_cobro", nullable = false)
    private LocalDateTime fechaCobro;

    @Column(name = "fecha_reembolso")
    private LocalDateTime fechaReembolso;
}
