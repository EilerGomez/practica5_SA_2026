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
import com.poc.orchestrator.models.saga.EntidadSagaPaso;
import lombok.Value;

import java.util.List;

@Value
public class SagaDetalleResponse {

    private SagaResponse saga;
    private List<SagaPasoResponse> pasos;

    public SagaDetalleResponse(EntidadSaga es, List<EntidadSagaPaso> listaPasos) {
        this.saga = new SagaResponse(es);
        this.pasos = listaPasos.stream().map(SagaPasoResponse::new).toList();
    }
}
