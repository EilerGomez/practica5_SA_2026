/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.controllers.simulacion;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoSimulacion.SimulacionRequest;
import com.poc.inventory.dtoSimulacion.SimulacionResponse;
import com.poc.inventory.services.simulacion.SimulacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/simulacion")
public class SimulacionController {

    private final SimulacionService simulacionService;

    public SimulacionController(SimulacionService simulacionService) {
        this.simulacionService = simulacionService;
    }

    @PostMapping
    public ResponseEntity<SimulacionResponse> configurar(
            @RequestBody SimulacionRequest request) {
        return ResponseEntity.ok(simulacionService.configurar(request));
    }

    @GetMapping
    public ResponseEntity<SimulacionResponse> estado() {
        return ResponseEntity.ok(simulacionService.estado());
    }
}
