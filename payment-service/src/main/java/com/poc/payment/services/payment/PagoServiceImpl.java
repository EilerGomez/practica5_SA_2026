/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.services.payment;

/**
 *
 * @author eiler
 */

import com.poc.payment.dtoPago.PagoRequest;
import com.poc.payment.dtoPago.PagoResponse;
import com.poc.payment.exceptions.PagoRechazadoException;
import com.poc.payment.models.payment.EntidadPago;
import com.poc.payment.models.payment.EstadoPago;
import com.poc.payment.repositories.payment.PagoRepository;
import com.poc.payment.services.simulacion.SimulacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final SimulacionService simulacionService;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           SimulacionService simulacionService) {
        this.pagoRepository = pagoRepository;
        this.simulacionService = simulacionService;
    }

    @Override
    @Transactional
    public PagoResponse cobrar(PagoRequest request) {
        aplicarRetraso();

        if (simulacionService.debeFallar()) {
            log.error("SIMULACION: cobro rechazado para saga={}", request.getSagaId());
            throw new PagoRechazadoException(
                    "Pago rechazado por el procesador para la saga " + request.getSagaId());
        }

        EntidadPago pago = new EntidadPago();
        pago.setSagaId(request.getSagaId());
        pago.setOrdenId(request.getOrdenId());
        pago.setCliente(request.getCliente());
        pago.setMonto(request.getMonto());
        pago.setEstado(EstadoPago.COBRADO);
        pago.setReferencia("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pago.setFechaCobro(LocalDateTime.now());

        EntidadPago guardado = pagoRepository.save(pago);
        log.info("Pago cobrado id={} saga={} monto={}",
                guardado.getId(), guardado.getSagaId(), guardado.getMonto());

        return new PagoResponse(guardado);
    }

    /**
     * Transaccion compensatoria. El cobro ocurrio y queda registrado;
     * lo que se hace es emitir un reembolso, no borrar el movimiento.
     */
    @Override
    @Transactional
    public PagoResponse reembolsar(Long id) {
        EntidadPago pago = buscar(id);

        if (pago.getEstado() == EstadoPago.REEMBOLSADO) {
            log.warn("Pago id={} ya estaba reembolsado, operacion idempotente", id);
            return new PagoResponse(pago);
        }

        pago.setEstado(EstadoPago.REEMBOLSADO);
        pago.setFechaReembolso(LocalDateTime.now());

        EntidadPago actualizado = pagoRepository.save(pago);
        log.info("COMPENSACION: pago id={} reembolsado monto={}", id, pago.getMonto());

        return new PagoResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(Long id) {
        return new PagoResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listar() {
        return pagoRepository.findAllByOrderByIdDesc()
                .stream()
                .map(PagoResponse::new)
                .toList();
    }

    private EntidadPago buscar(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pago no encontrado: " + id));
    }

    private void aplicarRetraso() {
        long ms = simulacionService.retrasoMs();
        if (ms <= 0) return;

        try {
            log.warn("SIMULACION: aplicando retraso de {} ms", ms);
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
