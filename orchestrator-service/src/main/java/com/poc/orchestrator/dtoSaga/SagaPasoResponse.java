/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.dtoSaga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.models.saga.EntidadSagaPaso;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SagaPasoResponse {

    private Long id;
    private String paso;
    private String tipo;
    private String estado;
    private String detalle;
    private Long recursoId;
    private LocalDateTime fechaEjecucion;

    public SagaPasoResponse(EntidadSagaPaso esp) {
        this.id = esp.getId();
        this.paso = esp.getPaso();
        this.tipo = esp.getTipo().name();
        this.estado = esp.getEstado().name();
        this.detalle = esp.getDetalle();
        this.recursoId = esp.getRecursoId();
        this.fechaEjecucion = esp.getFechaEjecucion();
    }
}
