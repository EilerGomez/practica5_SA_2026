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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoServiceImpl")
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private SimulacionService simulacionService;

    @InjectMocks
    private PagoServiceImpl pagoService;

    private PagoRequest request;

    @BeforeEach
    void setUp() {
        request = new PagoRequest();
        request.setSagaId("saga-001");
        request.setOrdenId(1L);
        request.setCliente("Eiler");
        request.setMonto(new BigDecimal("8500.00"));

        when(simulacionService.debeFallar()).thenReturn(false);
        when(simulacionService.retrasoMs()).thenReturn(0L);
    }

    @Test
    @DisplayName("cobrar() guarda el pago en estado COBRADO con referencia generada")
    void cobrarGuardaEnEstadoCobrado() {
        when(pagoRepository.save(any(EntidadPago.class)))
                .thenAnswer(inv -> {
                    EntidadPago e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        PagoResponse response = pagoService.cobrar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEstado()).isEqualTo("COBRADO");
        assertThat(response.getReferencia()).startsWith("REF-");
        assertThat(response.getFechaReembolso()).isNull();

        ArgumentCaptor<EntidadPago> captor = ArgumentCaptor.forClass(EntidadPago.class);
        verify(pagoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPago.COBRADO);
        assertThat(captor.getValue().getFechaCobro()).isNotNull();
    }

    @Test
    @DisplayName("cobrar() lanza PagoRechazadoException cuando la simulacion de fallo esta activa")
    void cobrarFallaConSimulacionActiva() {
        when(simulacionService.debeFallar()).thenReturn(true);

        assertThatThrownBy(() -> pagoService.cobrar(request))
                .isInstanceOf(PagoRechazadoException.class)
                .hasMessageContaining("saga-001");

        // Si el cobro falla, no debe quedar nada persistido
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cobrar() genera referencias distintas en cada llamada")
    void cobrarGeneraReferenciasUnicas() {
        when(pagoRepository.save(any(EntidadPago.class)))
                .thenAnswer(inv -> {
                    EntidadPago e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        String ref1 = pagoService.cobrar(request).getReferencia();
        String ref2 = pagoService.cobrar(request).getReferencia();

        assertThat(ref1).isNotEqualTo(ref2);
    }

    @Test
    @DisplayName("reembolsar() marca el pago como REEMBOLSADO sin borrarlo")
    void reembolsarMarcaComoReembolsado() {
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pagoCobrado()));
        when(pagoRepository.save(any(EntidadPago.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoResponse response = pagoService.reembolsar(1L);

        assertThat(response.getEstado()).isEqualTo("REEMBOLSADO");
        assertThat(response.getFechaReembolso()).isNotNull();
        // El cobro ocurrio y su fecha se conserva
        assertThat(response.getFechaCobro()).isNotNull();

        // La compensacion NO borra el movimiento
        verify(pagoRepository, never()).delete(any());
        verify(pagoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("reembolsar() es idempotente: no vuelve a guardar si ya estaba reembolsado")
    void reembolsarEsIdempotente() {
        EntidadPago yaReembolsado = pagoCobrado();
        yaReembolsado.setEstado(EstadoPago.REEMBOLSADO);
        yaReembolsado.setFechaReembolso(LocalDateTime.now());

        when(pagoRepository.findById(1L)).thenReturn(Optional.of(yaReembolsado));

        PagoResponse response = pagoService.reembolsar(1L);

        assertThat(response.getEstado()).isEqualTo("REEMBOLSADO");
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("reembolsar() lanza excepcion si el pago no existe")
    void reembolsarPagoInexistente() {
        when(pagoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.reembolsar(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("cobrar() aplica el retraso configurado en la simulacion")
    void cobrarAplicaRetraso() {
        when(simulacionService.retrasoMs()).thenReturn(300L);
        when(pagoRepository.save(any(EntidadPago.class)))
                .thenAnswer(inv -> {
                    EntidadPago e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        long inicio = System.currentTimeMillis();
        pagoService.cobrar(request);
        long transcurrido = System.currentTimeMillis() - inicio;

        assertThat(transcurrido).isGreaterThanOrEqualTo(300L);
    }

    @Test
    @DisplayName("listar() devuelve todos los pagos mapeados a DTO")
    void listarDevuelveTodos() {
        EntidadPago a = pagoCobrado();
        EntidadPago b = pagoCobrado();
        b.setId(2L);
        b.setSagaId("saga-002");

        when(pagoRepository.findAllByOrderByIdDesc()).thenReturn(List.of(b, a));

        List<PagoResponse> lista = pagoService.listar();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getSagaId()).isEqualTo("saga-002");
    }

    private EntidadPago pagoCobrado() {
        EntidadPago e = new EntidadPago();
        e.setId(1L);
        e.setSagaId("saga-001");
        e.setOrdenId(1L);
        e.setCliente("Eiler");
        e.setMonto(new BigDecimal("8500.00"));
        e.setEstado(EstadoPago.COBRADO);
        e.setReferencia("REF-ABC12345");
        e.setFechaCobro(LocalDateTime.now());
        return e;
    }
}
