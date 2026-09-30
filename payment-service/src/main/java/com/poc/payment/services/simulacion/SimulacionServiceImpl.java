/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.services.simulacion;

/**
 *
 * @author eiler
 */

import com.poc.payment.dtoSimulacion.SimulacionRequest;
import com.poc.payment.dtoSimulacion.SimulacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class SimulacionServiceImpl implements SimulacionService {

    private final AtomicBoolean falloActivo;
    private final AtomicLong retrasoMs;

    public SimulacionServiceImpl(
            @Value("${simulacion.fallo-activo:false}") boolean falloInicial,
            @Value("${simulacion.retraso-ms:0}") long retrasoInicial) {
        this.falloActivo = new AtomicBoolean(falloInicial);
        this.retrasoMs = new AtomicLong(retrasoInicial);
    }

    @Override
    public SimulacionResponse configurar(SimulacionRequest request) {
        if (request.getFalloActivo() != null) {
            falloActivo.set(request.getFalloActivo());
        }
        if (request.getRetrasoMs() != null) {
            retrasoMs.set(Math.max(0, request.getRetrasoMs()));
        }
        log.warn("SIMULACION actualizada: falloActivo={} retrasoMs={}",
                falloActivo.get(), retrasoMs.get());
        return estado();
    }

    @Override
    public SimulacionResponse estado() {
        return new SimulacionResponse(falloActivo.get(), retrasoMs.get());
    }

    @Override
    public boolean debeFallar() {
        return falloActivo.get();
    }

    @Override
    public long retrasoMs() {
        return retrasoMs.get();
    }
}
