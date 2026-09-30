/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.dtoInventario;

/**
 *
 * @author eiler
 */

import com.poc.inventory.models.inventory.EntidadReserva;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ReservaResponse {

    private Long id;
    private String sagaId;
    private Long ordenId;
    private String producto;
    private Integer cantidad;
    private String estado;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaLiberacion;

    public ReservaResponse(EntidadReserva er) {
        this.id = er.getId();
        this.sagaId = er.getSagaId();
        this.ordenId = er.getOrdenId();
        this.producto = er.getProducto();
        this.cantidad = er.getCantidad();
        this.estado = er.getEstado().name();
        this.fechaReserva = er.getFechaReserva();
        this.fechaLiberacion = er.getFechaLiberacion();
    }
}
