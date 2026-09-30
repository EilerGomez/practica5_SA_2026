/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.dtoEnvio;

/**
 *
 * @author eiler
 */
import com.poc.shipping.models.shipping.EntidadEnvio;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class EnvioResponse {

    private Long id;
    private String sagaId;
    private Long ordenId;
    private String cliente;
    private String direccion;
    private String guia;
    private String estado;
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaCancelacion;

    public EnvioResponse(EntidadEnvio ee) {
        this.id = ee.getId();
        this.sagaId = ee.getSagaId();
        this.ordenId = ee.getOrdenId();
        this.cliente = ee.getCliente();
        this.direccion = ee.getDireccion();
        this.guia = ee.getGuia();
        this.estado = ee.getEstado().name();
        this.fechaProgramada = ee.getFechaProgramada();
        this.fechaCancelacion = ee.getFechaCancelacion();
    }
}
