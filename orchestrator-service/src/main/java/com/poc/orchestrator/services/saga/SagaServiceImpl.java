/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.services.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.clients.inventory.InventoryClient;
import com.poc.orchestrator.clients.order.OrderClient;
import com.poc.orchestrator.clients.payment.PaymentClient;
import com.poc.orchestrator.clients.shipping.ShippingClient;
import com.poc.orchestrator.dtoClientes.*;
import com.poc.orchestrator.dtoSaga.CompraRequest;
import com.poc.orchestrator.dtoSaga.SagaDetalleResponse;
import com.poc.orchestrator.dtoSaga.SagaResponse;
import com.poc.orchestrator.exceptions.PasoFallidoException;
import com.poc.orchestrator.models.saga.*;
import com.poc.orchestrator.repositories.saga.SagaPasoRepository;
import com.poc.orchestrator.repositories.saga.SagaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class SagaServiceImpl implements SagaService {

    private final SagaRepository sagaRepository;
    private final SagaPasoRepository sagaPasoRepository;
    private final OrderClient orderClient;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final ShippingClient shippingClient;

    public SagaServiceImpl(SagaRepository sagaRepository,
                           SagaPasoRepository sagaPasoRepository,
                           OrderClient orderClient,
                           PaymentClient paymentClient,
                           InventoryClient inventoryClient,
                           ShippingClient shippingClient) {
        this.sagaRepository = sagaRepository;
        this.sagaPasoRepository = sagaPasoRepository;
        this.orderClient = orderClient;
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;
        this.shippingClient = shippingClient;
    }

    /**
     * Orquesta la saga de compra en cuatro pasos.
     * Si cualquiera falla, ejecuta las compensaciones en orden INVERSO.
     *
     * No lleva @Transactional: cada paso es una transaccion local en su
     * propio servicio. Aqui no hay nada que revertir automaticamente.
     */
    @Override
    public SagaResponse ejecutarCompra(CompraRequest request) {
        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        EntidadSaga saga = iniciarSaga(sagaId, request);

        log.info("===== INICIO SAGA {} =====", sagaId);

        try {
            // ---------- PASO 1: crear la orden ----------
            OrdenClienteResponse orden = orderClient.crear(
                    sagaId, request.getCliente(), request.getProducto(),
                    request.getCantidad(), request.getTotal());
            saga.setOrdenId(orden.getId());
            registrarPaso(sagaId, "ORDEN", TipoPaso.TRANSACCION, EstadoPaso.EXITOSO,
                    "Orden creada", orden.getId());
            sagaRepository.save(saga);

            // ---------- PASO 2: cobrar (Circuit Breaker) ----------
            PagoClienteResponse pago = paymentClient.cobrar(
                    sagaId, orden.getId(), request.getCliente(), request.getTotal());
            saga.setPagoId(pago.getId());
            registrarPaso(sagaId, "PAGO", TipoPaso.TRANSACCION, EstadoPaso.EXITOSO,
                    "Pago cobrado, referencia " + pago.getReferencia(), pago.getId());
            sagaRepository.save(saga);

            // ---------- PASO 3: reservar stock (Circuit Breaker) ----------
            ReservaClienteResponse reserva = inventoryClient.reservar(
                    sagaId, orden.getId(), request.getProducto(), request.getCantidad());
            saga.setReservaId(reserva.getId());
            registrarPaso(sagaId, "INVENTARIO", TipoPaso.TRANSACCION, EstadoPaso.EXITOSO,
                    "Stock reservado", reserva.getId());
            sagaRepository.save(saga);

            // ---------- PASO 4: programar el envio ----------
            EnvioClienteResponse envio = shippingClient.programar(
                    sagaId, orden.getId(), request.getCliente(), request.getDireccion());
            saga.setEnvioId(envio.getId());
            registrarPaso(sagaId, "ENVIO", TipoPaso.TRANSACCION, EstadoPaso.EXITOSO,
                    "Envio programado, guia " + envio.getGuia(), envio.getId());

            // ---------- Todo salio bien ----------
            saga.setEstado(EstadoSaga.COMPLETADA);
            saga.setFechaFin(LocalDateTime.now());
            sagaRepository.save(saga);

            log.info("===== SAGA {} COMPLETADA =====", sagaId);
            return new SagaResponse(saga);

        } catch (PasoFallidoException e) {
            log.error("SAGA {} fallo en el paso {}: {}", sagaId, e.getPaso(), e.getMessage());

            registrarPaso(sagaId, e.getPaso(), TipoPaso.TRANSACCION,
                    e.getMessage().contains("Circuito abierto")
                            ? EstadoPaso.CIRCUITO_ABIERTO : EstadoPaso.FALLIDO,
                    e.getMessage(), null);

            saga.setMotivoFallo(e.getMessage());
            compensar(saga);
            return new SagaResponse(saga);

        } catch (Exception e) {
            log.error("SAGA {} fallo inesperado: {}", sagaId, e.getMessage(), e);

            registrarPaso(sagaId, "DESCONOCIDO", TipoPaso.TRANSACCION, EstadoPaso.FALLIDO,
                    e.getMessage(), null);

            saga.setMotivoFallo("Error inesperado: " + e.getMessage());
            compensar(saga);
            return new SagaResponse(saga);
        }
    }

    /**
     * Ejecuta las compensaciones en ORDEN INVERSO al de las transacciones.
     * Cada compensacion se intenta de forma independiente: si una falla,
     * las demas siguen ejecutandose.
     */
    private void compensar(EntidadSaga saga) {
        String sagaId = saga.getSagaId();
        log.warn("===== INICIANDO COMPENSACION DE {} =====", sagaId);

        saga.setEstado(EstadoSaga.COMPENSANDO);
        sagaRepository.save(saga);

        boolean todoCompensado = true;

        // Orden inverso: ENVIO -> INVENTARIO -> PAGO -> ORDEN

        if (saga.getEnvioId() != null) {
            todoCompensado &= compensarPaso(sagaId, "ENVIO", saga.getEnvioId(),
                    () -> shippingClient.cancelar(saga.getEnvioId()));
        }

        if (saga.getReservaId() != null) {
            todoCompensado &= compensarPaso(sagaId, "INVENTARIO", saga.getReservaId(),
                    () -> inventoryClient.liberar(saga.getReservaId()));
        }

        if (saga.getPagoId() != null) {
            todoCompensado &= compensarPaso(sagaId, "PAGO", saga.getPagoId(),
                    () -> paymentClient.reembolsar(saga.getPagoId()));
        }

        if (saga.getOrdenId() != null) {
            todoCompensado &= compensarPaso(sagaId, "ORDEN", saga.getOrdenId(),
                    () -> orderClient.cancelar(saga.getOrdenId()));
        }

        saga.setEstado(todoCompensado ? EstadoSaga.COMPENSADA : EstadoSaga.FALLIDA);
        saga.setFechaFin(LocalDateTime.now());
        sagaRepository.save(saga);

        if (todoCompensado) {
            log.warn("===== SAGA {} COMPENSADA CORRECTAMENTE =====", sagaId);
        } else {
            log.error("===== SAGA {} QUEDO INCONSISTENTE: revisar manualmente =====", sagaId);
        }
    }

    /**
     * Ejecuta una compensacion individual. Devuelve false si fallo.
     * Una compensacion fallida es el peor escenario de una saga:
     * ningun patron lo resuelve solo, requiere intervencion humana.
     */
    private boolean compensarPaso(String sagaId, String paso, Long recursoId,
                                  Runnable accion) {
        try {
            accion.run();
            registrarPaso(sagaId, paso, TipoPaso.COMPENSACION, EstadoPaso.EXITOSO,
                    "Compensacion ejecutada", recursoId);
            return true;
        } catch (Exception e) {
            log.error("COMPENSACION FALLIDA en {} para saga {}: {}", paso, sagaId, e.getMessage());
            registrarPaso(sagaId, paso, TipoPaso.COMPENSACION, EstadoPaso.FALLIDO,
                    "Fallo la compensacion: " + e.getMessage(), recursoId);
            return false;
        }
    }

    private EntidadSaga iniciarSaga(String sagaId, CompraRequest request) {
        EntidadSaga saga = new EntidadSaga();
        saga.setSagaId(sagaId);
        saga.setCliente(request.getCliente());
        saga.setProducto(request.getProducto());
        saga.setCantidad(request.getCantidad());
        saga.setTotal(request.getTotal());
        saga.setDireccion(request.getDireccion());
        saga.setEstado(EstadoSaga.INICIADA);
        saga.setFechaInicio(LocalDateTime.now());
        return sagaRepository.save(saga);
    }

    private void registrarPaso(String sagaId, String paso, TipoPaso tipo,
                               EstadoPaso estado, String detalle, Long recursoId) {
        EntidadSagaPaso registro = new EntidadSagaPaso();
        registro.setSagaId(sagaId);
        registro.setPaso(paso);
        registro.setTipo(tipo);
        registro.setEstado(estado);
        registro.setDetalle(detalle != null && detalle.length() > 500
                ? detalle.substring(0, 500) : detalle);
        registro.setRecursoId(recursoId);
        registro.setFechaEjecucion(LocalDateTime.now());
        sagaPasoRepository.save(registro);
    }

    @Override
    @Transactional(readOnly = true)
    public SagaDetalleResponse obtenerPorSagaId(String sagaId) {
        EntidadSaga saga = sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new NoSuchElementException("Saga no encontrada: " + sagaId));
        List<EntidadSagaPaso> pasos = sagaPasoRepository.findBySagaIdOrderByIdAsc(sagaId);
        return new SagaDetalleResponse(saga, pasos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SagaResponse> listar() {
        return sagaRepository.findAllByOrderByIdDesc()
                .stream()
                .map(SagaResponse::new)
                .toList();
    }
}
