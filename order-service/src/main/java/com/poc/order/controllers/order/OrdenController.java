/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.order.controllers.order;

/**
 *
 * @author eiler
 */

import com.poc.order.dtoOrden.OrdenRequest;
import com.poc.order.dtoOrden.OrdenResponse;
import com.poc.order.services.order.OrdenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/orders")
public class OrdenController {

    private final OrdenService ordenService;
    
    @Autowired
    public OrdenController(OrdenService ordenService){
        this.ordenService=ordenService;
    }

    @PostMapping
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody OrdenRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ordenService.crear(request));
    }

    /** Transaccion compensatoria de la saga. */
    @DeleteMapping("/{id}")
    public ResponseEntity<OrdenResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.cancelar(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrdenResponse> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.confirmar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<OrdenResponse>> listar() {
        return ResponseEntity.ok(ordenService.listar());
    }
}
