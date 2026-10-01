/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.dtoSaga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.models.saga.EntidadSaga;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class SagaResponse {

    private String sagaId;
    private String cliente;
    private String producto;
    private Integer cantidad;
    private BigDecimal total;
    private String estado;
    private String motivoFallo;
    private Long ordenId;
    private Long pagoId;
    private Long reservaId;
    private Long envioId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    public SagaResponse(EntidadSaga es) {
        this.sagaId = es.getSagaId();
        this.cliente = es.getCliente();
        this.producto = es.getProducto();
        this.cantidad = es.getCantidad();
        this.total = es.getTotal();
        this.estado = es.getEstado().name();
        this.motivoFallo = es.getMotivoFallo();
        this.ordenId = es.getOrdenId();
        this.pagoId = es.getPagoId();
        this.reservaId = es.getReservaId();
        this.envioId = es.getEnvioId();
        this.fechaInicio = es.getFechaInicio();
        this.fechaFin = es.getFechaFin();
    }
}
