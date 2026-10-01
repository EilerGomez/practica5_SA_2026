/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.controllers.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.dtoSaga.CompraRequest;
import com.poc.orchestrator.dtoSaga.SagaDetalleResponse;
import com.poc.orchestrator.dtoSaga.SagaResponse;
import com.poc.orchestrator.services.saga.SagaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/saga")
@CrossOrigin(origins = "*")
public class SagaController {

    private final SagaService sagaService;

    public SagaController(SagaService sagaService) {
        this.sagaService = sagaService;
    }

    /** Punto de entrada: ejecuta la saga completa de compra. */
    @PostMapping("/compra")
    public ResponseEntity<SagaResponse> comprar(@Valid @RequestBody CompraRequest request) {
        return ResponseEntity.ok(sagaService.ejecutarCompra(request));
    }

    /** Traza completa de una saga, paso a paso. */
    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaDetalleResponse> detalle(@PathVariable String sagaId) {
        return ResponseEntity.ok(sagaService.obtenerPorSagaId(sagaId));
    }

    @GetMapping
    public ResponseEntity<List<SagaResponse>> listar() {
        return ResponseEntity.ok(sagaService.listar());
    }
}
