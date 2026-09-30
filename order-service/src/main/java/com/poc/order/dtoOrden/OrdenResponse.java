/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.order.dtoOrden;

/**
 *
 * @author eiler
 */

import com.poc.order.models.orden.EntidadOrden;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class OrdenResponse {

    private Long id;
    private String sagaId;
    private String cliente;
    private String producto;
    private Integer cantidad;
    private BigDecimal total;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCancelacion;

    public OrdenResponse(EntidadOrden eo) {
        this.id = eo.getId();
        this.sagaId = eo.getSagaId();
        this.cliente = eo.getCliente();
        this.producto = eo.getProducto();
        this.cantidad = eo.getCantidad();
        this.total = eo.getTotal();
        this.estado = eo.getEstado().name();
        this.fechaCreacion = eo.getFechaCreacion();
        this.fechaCancelacion = eo.getFechaCancelacion();
    }
}