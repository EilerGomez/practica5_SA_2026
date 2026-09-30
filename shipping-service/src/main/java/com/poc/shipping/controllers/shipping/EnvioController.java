/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.controllers.shipping;

/**
 *
 * @author eiler
 */
import com.poc.shipping.dtoEnvio.EnvioRequest;
import com.poc.shipping.dtoEnvio.EnvioResponse;
import com.poc.shipping.services.shipping.EnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping")
public class EnvioController {

    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    @PostMapping("/schedule")
    public ResponseEntity<EnvioResponse> programar(@Valid @RequestBody EnvioRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(envioService.programar(request));
    }

    /** Transaccion compensatoria de la saga. */
    @DeleteMapping("/{id}")
    public ResponseEntity<EnvioResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.cancelar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<EnvioResponse>> listar() {
        return ResponseEntity.ok(envioService.listar());
    }
}
