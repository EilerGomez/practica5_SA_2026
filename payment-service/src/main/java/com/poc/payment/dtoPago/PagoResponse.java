/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.dtoPago;

/**
 *
 * @author eiler
 */

import com.poc.payment.models.payment.EntidadPago;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class PagoResponse {

    private Long id;
    private String sagaId;
    private Long ordenId;
    private String cliente;
    private BigDecimal monto;
    private String estado;
    private String referencia;
    private LocalDateTime fechaCobro;
    private LocalDateTime fechaReembolso;

    public PagoResponse(EntidadPago ep) {
        this.id = ep.getId();
        this.sagaId = ep.getSagaId();
        this.ordenId = ep.getOrdenId();
        this.cliente = ep.getCliente();
        this.monto = ep.getMonto();
        this.estado = ep.getEstado().name();
        this.referencia = ep.getReferencia();
        this.fechaCobro = ep.getFechaCobro();
        this.fechaReembolso = ep.getFechaReembolso();
    }
}
