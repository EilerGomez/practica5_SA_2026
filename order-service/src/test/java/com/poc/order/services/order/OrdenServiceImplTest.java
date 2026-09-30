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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdenServiceImpl")
class OrdenServiceImplTest {

    @Mock
    private OrdenRepository ordenRepository;

    @InjectMocks
    private OrdenServiceImpl ordenService;

    private OrdenRequest request;

    @BeforeEach
    void setUp() {
        request = new OrdenRequest();
        request.setSagaId("saga-001");
        request.setCliente("Eiler");
        request.setProducto("Laptop");
        request.setCantidad(1);
        request.setTotal(new BigDecimal("8500.00"));
    }

    @Test
    @DisplayName("crear() guarda la orden en estado CREADA")
    void crearGuardaEnEstadoCreada() {
        when(ordenRepository.save(any(EntidadOrden.class)))
                .thenAnswer(inv -> {
                    EntidadOrden e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        OrdenResponse response = ordenService.crear(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEstado()).isEqualTo("CREADA");
        assertThat(response.getSagaId()).isEqualTo("saga-001");
        assertThat(response.getFechaCancelacion()).isNull();

        ArgumentCaptor<EntidadOrden> captor = ArgumentCaptor.forClass(EntidadOrden.class);
        verify(ordenRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOrden.CREADA);
        assertThat(captor.getValue().getFechaCreacion()).isNotNull();
    }

    @Test
    @DisplayName("cancelar() marca la orden como CANCELADA sin borrarla")
    void cancelarMarcaComoCancelada() {
        EntidadOrden existente = ordenCreada();
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(ordenRepository.save(any(EntidadOrden.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenResponse response = ordenService.cancelar(1L);

        assertThat(response.getEstado()).isEqualTo("CANCELADA");
        assertThat(response.getFechaCancelacion()).isNotNull();

        // La compensacion NO borra: es un cambio de estado, no un rollback
        verify(ordenRepository, never()).delete(any());
        verify(ordenRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("cancelar() es idempotente: no vuelve a guardar si ya estaba cancelada")
    void cancelarEsIdempotente() {
        EntidadOrden yaCancelada = ordenCreada();
        yaCancelada.setEstado(EstadoOrden.CANCELADA);
        yaCancelada.setFechaCancelacion(LocalDateTime.now());

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(yaCancelada));

        OrdenResponse response = ordenService.cancelar(1L);

        assertThat(response.getEstado()).isEqualTo("CANCELADA");
        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmar() cambia el estado a CONFIRMADA")
    void confirmarCambiaEstado() {
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(ordenCreada()));
        when(ordenRepository.save(any(EntidadOrden.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenResponse response = ordenService.confirmar(1L);

        assertThat(response.getEstado()).isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName("cancelar() lanza excepcion si la orden no existe")
    void cancelarOrdenInexistente() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenService.cancelar(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("listar() devuelve todas las ordenes mapeadas a DTO")
    void listarDevuelveTodas() {
        EntidadOrden a = ordenCreada();
        EntidadOrden b = ordenCreada();
        b.setId(2L);
        b.setSagaId("saga-002");

        when(ordenRepository.findAllByOrderByIdDesc()).thenReturn(List.of(b, a));

        List<OrdenResponse> lista = ordenService.listar();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getSagaId()).isEqualTo("saga-002");
    }

    private EntidadOrden ordenCreada() {
        EntidadOrden e = new EntidadOrden();
        e.setId(1L);
        e.setSagaId("saga-001");
        e.setCliente("Eiler");
        e.setProducto("Laptop");
        e.setCantidad(1);
        e.setTotal(new BigDecimal("8500.00"));
        e.setEstado(EstadoOrden.CREADA);
        e.setFechaCreacion(LocalDateTime.now());
        return e;
    }
}