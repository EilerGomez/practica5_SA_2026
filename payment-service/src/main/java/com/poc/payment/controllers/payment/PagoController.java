/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.controllers.payment;

/**
 *
 * @author eiler
 */

import com.poc.payment.dtoPago.PagoRequest;
import com.poc.payment.dtoPago.PagoResponse;
import com.poc.payment.services.payment.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> cobrar(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pagoService.cobrar(request));
    }

    /** Transaccion compensatoria de la saga. */
    @PostMapping("/{id}/refund")
    public ResponseEntity<PagoResponse> reembolsar(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.reembolsar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> listar() {
        return ResponseEntity.ok(pagoService.listar());
    }
}
