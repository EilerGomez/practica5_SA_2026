/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.controllers.inventory;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoInventario.ProductoResponse;
import com.poc.inventory.dtoInventario.ReservaRequest;
import com.poc.inventory.dtoInventario.ReservaResponse;
import com.poc.inventory.services.inventory.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventarioService.reservar(request));
    }

    /** Transaccion compensatoria de la saga. */
    @PostMapping("/release/{id}")
    public ResponseEntity<ReservaResponse> liberar(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.liberar(id));
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<ReservaResponse> obtenerReserva(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.obtenerReserva(id));
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservaResponse>> listarReservas() {
        return ResponseEntity.ok(inventarioService.listarReservas());
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductoResponse>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }
}
