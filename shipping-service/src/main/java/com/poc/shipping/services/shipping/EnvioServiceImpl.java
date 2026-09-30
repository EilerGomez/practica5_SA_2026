/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.services.shipping;

/**
 *
 * @author eiler
 */
import com.poc.shipping.dtoEnvio.EnvioRequest;
import com.poc.shipping.dtoEnvio.EnvioResponse;
import com.poc.shipping.exceptions.EnvioNoDisponibleException;
import com.poc.shipping.models.shipping.EntidadEnvio;
import com.poc.shipping.models.shipping.EstadoEnvio;
import com.poc.shipping.repositories.shipping.EnvioRepository;
import com.poc.shipping.services.simulacion.SimulacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class EnvioServiceImpl implements EnvioService {

    private final EnvioRepository envioRepository;
    private final SimulacionService simulacionService;

    public EnvioServiceImpl(EnvioRepository envioRepository,
                            SimulacionService simulacionService) {
        this.envioRepository = envioRepository;
        this.simulacionService = simulacionService;
    }

    @Override
    @Transactional
    public EnvioResponse programar(EnvioRequest request) {
        aplicarRetraso();

        if (simulacionService.debeFallar()) {
            log.error("SIMULACION: envio no programado para saga={}", request.getSagaId());
            throw new EnvioNoDisponibleException(
                    "Servicio de envios no disponible temporalmente");
        }

        EntidadEnvio envio = new EntidadEnvio();
        envio.setSagaId(request.getSagaId());
        envio.setOrdenId(request.getOrdenId());
        envio.setCliente(request.getCliente());
        envio.setDireccion(request.getDireccion());
        envio.setGuia("GUIA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        envio.setEstado(EstadoEnvio.PROGRAMADO);
        // Entrega estimada: 3 dias habiles
        envio.setFechaProgramada(LocalDateTime.now().plusDays(3));

        EntidadEnvio guardado = envioRepository.save(envio);
        log.info("Envio programado id={} saga={} guia={}",
                guardado.getId(), guardado.getSagaId(), guardado.getGuia());

        return new EnvioResponse(guardado);
    }

    /**
     * Transaccion compensatoria: cancela el envio programado.
     */
    @Override
    @Transactional
    public EnvioResponse cancelar(Long id) {
        EntidadEnvio envio = buscar(id);

        if (envio.getEstado() == EstadoEnvio.CANCELADO) {
            log.warn("Envio id={} ya estaba cancelado, operacion idempotente", id);
            return new EnvioResponse(envio);
        }

        envio.setEstado(EstadoEnvio.CANCELADO);
        envio.setFechaCancelacion(LocalDateTime.now());

        EntidadEnvio actualizado = envioRepository.save(envio);
        log.info("COMPENSACION: envio id={} cancelado guia={}", id, envio.getGuia());

        return new EnvioResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioResponse obtenerPorId(Long id) {
        return new EnvioResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvioResponse> listar() {
        return envioRepository.findAllByOrderByIdDesc()
                .stream()
                .map(EnvioResponse::new)
                .toList();
    }

    private EntidadEnvio buscar(Long id) {
        return envioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Envio no encontrado: " + id));
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
