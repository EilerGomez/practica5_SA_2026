/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.order.services.order;

/**
 *
 * @author eiler
 */

import com.poc.order.dtoOrden.OrdenRequest;
import com.poc.order.dtoOrden.OrdenResponse;
import com.poc.order.models.orden.EntidadOrden;
import com.poc.order.models.orden.EstadoOrden;
import com.poc.order.repositories.orden.OrdenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class OrdenServiceImpl implements OrdenService {

    private final OrdenRepository ordenRepository;
    
    @Autowired
    public OrdenServiceImpl(OrdenRepository repositoryOrden){
        this.ordenRepository=repositoryOrden;
    }

    @Override
    @Transactional
    public OrdenResponse crear(OrdenRequest request) {
        EntidadOrden orden = new EntidadOrden();
        orden.setSagaId(request.getSagaId());
        orden.setCliente(request.getCliente());
        orden.setProducto(request.getProducto());
        orden.setCantidad(request.getCantidad());
        orden.setTotal(request.getTotal());
        orden.setEstado(EstadoOrden.CREADA);
        orden.setFechaCreacion(LocalDateTime.now());

        EntidadOrden guardada = ordenRepository.save(orden);
        log.info("Orden creada id={} saga={}", guardada.getId(), guardada.getSagaId());

        return new OrdenResponse(guardada);
    }

    /**
     * Transaccion compensatoria. No borra el registro: lo marca como
     * CANCELADA para conservar la trazabilidad de la saga.
     */
    @Override
    @Transactional
    public OrdenResponse cancelar(Long id) {
        EntidadOrden orden = buscar(id);

        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            log.warn("Orden id={} ya estaba cancelada, operacion idempotente", id);
            return new OrdenResponse(orden);
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setFechaCancelacion(LocalDateTime.now());

        EntidadOrden actualizada = ordenRepository.save(orden);
        log.info("COMPENSACION: orden id={} cancelada", id);

        return new OrdenResponse(actualizada);
    }

    @Override
    @Transactional
    public OrdenResponse confirmar(Long id) {
        EntidadOrden orden = buscar(id);
        orden.setEstado(EstadoOrden.CONFIRMADA);
        return new OrdenResponse(ordenRepository.save(orden));
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenResponse obtenerPorId(Long id) {
        return new OrdenResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenResponse> listar() {
        return ordenRepository.findAllByOrderByIdDesc()
                .stream()
                .map(OrdenResponse::new)
                .toList();
    }

    private EntidadOrden buscar(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Orden no encontrada: " + id));
    }
}