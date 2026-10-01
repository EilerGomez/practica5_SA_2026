/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.orchestrator.services.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.dtoSaga.CompraRequest;
import com.poc.orchestrator.dtoSaga.SagaDetalleResponse;
import com.poc.orchestrator.dtoSaga.SagaResponse;

import java.util.List;

public interface SagaService {

    SagaResponse ejecutarCompra(CompraRequest request);

    SagaDetalleResponse obtenerPorSagaId(String sagaId);

    List<SagaResponse> listar();
}
