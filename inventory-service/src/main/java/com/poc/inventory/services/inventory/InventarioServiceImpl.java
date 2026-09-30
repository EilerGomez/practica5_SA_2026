/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.services.inventory;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoInventario.ProductoResponse;
import com.poc.inventory.dtoInventario.ReservaRequest;
import com.poc.inventory.dtoInventario.ReservaResponse;
import com.poc.inventory.exceptions.InventarioNoDisponibleException;
import com.poc.inventory.exceptions.StockInsuficienteException;
import com.poc.inventory.models.inventory.EntidadProducto;
import com.poc.inventory.models.inventory.EntidadReserva;
import com.poc.inventory.models.inventory.EstadoReserva;
import com.poc.inventory.repositories.inventory.ProductoRepository;
import com.poc.inventory.repositories.inventory.ReservaRepository;
import com.poc.inventory.services.simulacion.SimulacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class InventarioServiceImpl implements InventarioService {

    private final ProductoRepository productoRepository;
    private final ReservaRepository reservaRepository;
    private final SimulacionService simulacionService;

    public InventarioServiceImpl(ProductoRepository productoRepository,
                                 ReservaRepository reservaRepository,
                                 SimulacionService simulacionService) {
        this.productoRepository = productoRepository;
        this.reservaRepository = reservaRepository;
        this.simulacionService = simulacionService;
    }

    @Override
    @Transactional
    public ReservaResponse reservar(ReservaRequest request) {
        aplicarRetraso();

        // Fallo de infraestructura simulado
        if (simulacionService.debeFallar()) {
            log.error("SIMULACION: inventario no disponible para saga={}", request.getSagaId());
            throw new InventarioNoDisponibleException(
                    "Servicio de inventario no disponible temporalmente");
        }

        EntidadProducto producto = productoRepository
                .findByNombreConBloqueo(request.getProducto())
                .orElseThrow(() -> new NoSuchElementException(
                        "Producto no encontrado: " + request.getProducto()));

        // Fallo de negocio real
        if (producto.getStockDisponible() < request.getCantidad()) {
            log.warn("Stock insuficiente producto={} solicitado={} disponible={}",
                    producto.getNombre(), request.getCantidad(), producto.getStockDisponible());
            throw new StockInsuficienteException(String.format(
                    "Stock insuficiente para %s. Solicitado: %d, disponible: %d",
                    producto.getNombre(), request.getCantidad(), producto.getStockDisponible()));
        }

        producto.setStockReservado(producto.getStockReservado() + request.getCantidad());
        productoRepository.save(producto);

        EntidadReserva reserva = new EntidadReserva();
        reserva.setSagaId(request.getSagaId());
        reserva.setOrdenId(request.getOrdenId());
        reserva.setProducto(request.getProducto());
        reserva.setCantidad(request.getCantidad());
        reserva.setEstado(EstadoReserva.RESERVADO);
        reserva.setFechaReserva(LocalDateTime.now());

        EntidadReserva guardada = reservaRepository.save(reserva);
        log.info("Reserva creada id={} saga={} producto={} cantidad={} disponible ahora={}",
                guardada.getId(), guardada.getSagaId(), guardada.getProducto(),
                guardada.getCantidad(), producto.getStockDisponible());

        return new ReservaResponse(guardada);
    }

    /**
     * Transaccion compensatoria: devuelve las unidades al stock disponible.
     */
    @Override
    @Transactional
    public ReservaResponse liberar(Long id) {
        EntidadReserva reserva = buscarReserva(id);

        if (reserva.getEstado() == EstadoReserva.LIBERADO) {
            log.warn("Reserva id={} ya estaba liberada, operacion idempotente", id);
            return new ReservaResponse(reserva);
        }

        EntidadProducto producto = productoRepository
                .findByNombreConBloqueo(reserva.getProducto())
                .orElseThrow(() -> new NoSuchElementException(
                        "Producto no encontrado: " + reserva.getProducto()));

        producto.setStockReservado(
                Math.max(0, producto.getStockReservado() - reserva.getCantidad()));
        productoRepository.save(producto);

        reserva.setEstado(EstadoReserva.LIBERADO);
        reserva.setFechaLiberacion(LocalDateTime.now());
        EntidadReserva actualizada = reservaRepository.save(reserva);

        log.info("COMPENSACION: reserva id={} liberada, {} unidades devueltas a {}",
                id, reserva.getCantidad(), producto.getNombre());

        return new ReservaResponse(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerReserva(Long id) {
        return new ReservaResponse(buscarReserva(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarReservas() {
        return reservaRepository.findAllByOrderByIdDesc()
                .stream()
                .map(ReservaResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos() {
        return productoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::new)
                .toList();
    }

    private EntidadReserva buscarReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada: " + id));
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
