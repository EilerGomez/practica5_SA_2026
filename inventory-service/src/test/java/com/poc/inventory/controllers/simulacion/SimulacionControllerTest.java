/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.controllers.simulacion;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoSimulacion.SimulacionRequest;
import com.poc.inventory.dtoSimulacion.SimulacionResponse;
import com.poc.inventory.services.simulacion.SimulacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulacionController.class)
@DisplayName("SimulacionController")
class SimulacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimulacionService simulacionService;

    @Test
    @DisplayName("GET /simulacion devuelve el estado actual")
    void estadoDevuelveConfiguracionActual() throws Exception {
        when(simulacionService.estado())
                .thenReturn(new SimulacionResponse(false, 0L));

        mockMvc.perform(get("/simulacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.falloActivo").value(false))
                .andExpect(jsonPath("$.retrasoMs").value(0));
    }

    @Test
    @DisplayName("POST /simulacion activa el fallo de infraestructura")
    void configurarActivaFallo() throws Exception {
        when(simulacionService.configurar(any(SimulacionRequest.class)))
                .thenReturn(new SimulacionResponse(true, 0L));

        String body = """
                { "falloActivo": true }
                """;

        mockMvc.perform(post("/simulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.falloActivo").value(true));
    }

    @Test
    @DisplayName("POST /simulacion configura el retraso")
    void configurarEstableceRetraso() throws Exception {
        when(simulacionService.configurar(any(SimulacionRequest.class)))
                .thenReturn(new SimulacionResponse(false, 3000L));

        String body = """
                { "falloActivo": false, "retrasoMs": 3000 }
                """;

        mockMvc.perform(post("/simulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrasoMs").value(3000));
    }

    @Test
    @DisplayName("POST /simulacion traslada correctamente los valores al servicio")
    void configurarTrasladaValoresAlServicio() throws Exception {
        when(simulacionService.configurar(any(SimulacionRequest.class)))
                .thenReturn(new SimulacionResponse(true, 1500L));

        String body = """
                { "falloActivo": true, "retrasoMs": 1500 }
                """;

        mockMvc.perform(post("/simulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<SimulacionRequest> captor =
                ArgumentCaptor.forClass(SimulacionRequest.class);
        verify(simulacionService).configurar(captor.capture());

        assertThat(captor.getValue().getFalloActivo()).isTrue();
        assertThat(captor.getValue().getRetrasoMs()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("POST /simulacion acepta un cuerpo con campos parciales")
    void configurarAceptaCamposParciales() throws Exception {
        when(simulacionService.configurar(any(SimulacionRequest.class)))
                .thenReturn(new SimulacionResponse(false, 500L));

        String soloRetraso = """
                { "retrasoMs": 500 }
                """;

        mockMvc.perform(post("/simulacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(soloRetraso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrasoMs").value(500));

        ArgumentCaptor<SimulacionRequest> captor =
                ArgumentCaptor.forClass(SimulacionRequest.class);
        verify(simulacionService).configurar(captor.capture());

        assertThat(captor.getValue().getFalloActivo()).isNull();
        assertThat(captor.getValue().getRetrasoMs()).isEqualTo(500L);
    }
}
