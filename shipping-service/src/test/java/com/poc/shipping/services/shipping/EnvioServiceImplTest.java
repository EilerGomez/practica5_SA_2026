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
@DisplayName("EnvioServiceImpl")
class EnvioServiceImplTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private SimulacionService simulacionService;

    @InjectMocks
    private EnvioServiceImpl envioService;

    private EnvioRequest request;

    @BeforeEach
    void setUp() {
        request = new EnvioRequest();
        request.setSagaId("saga-001");
        request.setOrdenId(1L);
        request.setCliente("Eiler");
        request.setDireccion("Zona 3, Quetzaltenango");

        when(simulacionService.debeFallar()).thenReturn(false);
        when(simulacionService.retrasoMs()).thenReturn(0L);
    }

    @Test
    @DisplayName("programar() crea el envio con guia y fecha estimada")
    void programarCreaEnvio() {
        when(envioRepository.save(any(EntidadEnvio.class)))
                .thenAnswer(inv -> {
                    EntidadEnvio e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        EnvioResponse response = envioService.programar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEstado()).isEqualTo("PROGRAMADO");
        assertThat(response.getGuia()).startsWith("GUIA-");
        assertThat(response.getFechaProgramada()).isAfter(LocalDateTime.now());
        assertThat(response.getFechaCancelacion()).isNull();

        ArgumentCaptor<EntidadEnvio> captor = ArgumentCaptor.forClass(EntidadEnvio.class);
        verify(envioRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEnvio.PROGRAMADO);
    }

    @Test
    @DisplayName("programar() genera guias distintas en cada llamada")
    void programarGeneraGuiasUnicas() {
        when(envioRepository.save(any(EntidadEnvio.class)))
                .thenAnswer(inv -> {
                    EntidadEnvio e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        String guia1 = envioService.programar(request).getGuia();
        String guia2 = envioService.programar(request).getGuia();

        assertThat(guia1).isNotEqualTo(guia2);
    }

    @Test
    @DisplayName("programar() lanza EnvioNoDisponibleException con la simulacion activa")
    void programarFallaConSimulacionActiva() {
        when(simulacionService.debeFallar()).thenReturn(true);

        assertThatThrownBy(() -> envioService.programar(request))
                .isInstanceOf(EnvioNoDisponibleException.class);

        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("programar() aplica el retraso configurado")
    void programarAplicaRetraso() {
        when(simulacionService.retrasoMs()).thenReturn(300L);
        when(envioRepository.save(any(EntidadEnvio.class)))
                .thenAnswer(inv -> {
                    EntidadEnvio e = inv.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        long inicio = System.currentTimeMillis();
        envioService.programar(request);
        long transcurrido = System.currentTimeMillis() - inicio;

        assertThat(transcurrido).isGreaterThanOrEqualTo(300L);
    }

    @Test
    @DisplayName("cancelar() marca el envio como CANCELADO sin borrarlo")
    void cancelarMarcaComoCancelado() {
        when(envioRepository.findById(1L)).thenReturn(Optional.of(envioProgramado()));
        when(envioRepository.save(any(EntidadEnvio.class))).thenAnswer(inv -> inv.getArgument(0));

        EnvioResponse response = envioService.cancelar(1L);

        assertThat(response.getEstado()).isEqualTo("CANCELADO");
        assertThat(response.getFechaCancelacion()).isNotNull();
        assertThat(response.getGuia()).isNotBlank();

        verify(envioRepository, never()).delete(any());
        verify(envioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("cancelar() es idempotente: no vuelve a guardar si ya estaba cancelado")
    void cancelarEsIdempotente() {
        EntidadEnvio yaCancelado = envioProgramado();
        yaCancelado.setEstado(EstadoEnvio.CANCELADO);
        yaCancelado.setFechaCancelacion(LocalDateTime.now());

        when(envioRepository.findById(1L)).thenReturn(Optional.of(yaCancelado));

        EnvioResponse response = envioService.cancelar(1L);

        assertThat(response.getEstado()).isEqualTo("CANCELADO");
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar() lanza excepcion si el envio no existe")
    void cancelarEnvioInexistente() {
        when(envioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envioService.cancelar(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("listar() devuelve los envios mapeados a DTO")
    void listarDevuelveTodos() {
        EntidadEnvio a = envioProgramado();
        EntidadEnvio b = envioProgramado();
        b.setId(2L);
        b.setSagaId("saga-002");

        when(envioRepository.findAllByOrderByIdDesc()).thenReturn(List.of(b, a));

        List<EnvioResponse> lista = envioService.listar();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getSagaId()).isEqualTo("saga-002");
    }

    private EntidadEnvio envioProgramado() {
        EntidadEnvio e = new EntidadEnvio();
        e.setId(1L);
        e.setSagaId("saga-001");
        e.setOrdenId(1L);
        e.setCliente("Eiler");
        e.setDireccion("Zona 3, Quetzaltenango");
        e.setGuia("GUIA-ABC12345");
        e.setEstado(EstadoEnvio.PROGRAMADO);
        e.setFechaProgramada(LocalDateTime.now().plusDays(3));
        return e;
    }
}
