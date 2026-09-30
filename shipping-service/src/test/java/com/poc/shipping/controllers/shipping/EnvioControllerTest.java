/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.controllers.shipping;

/**
 *
 * @author eiler
 */
import com.poc.shipping.controllers.advice.ManejadorErrores;
import com.poc.shipping.dtoEnvio.EnvioRequest;
import com.poc.shipping.dtoEnvio.EnvioResponse;
import com.poc.shipping.exceptions.EnvioNoDisponibleException;
import com.poc.shipping.models.shipping.EntidadEnvio;
import com.poc.shipping.models.shipping.EstadoEnvio;
import com.poc.shipping.services.shipping.EnvioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnvioController.class)
@Import(ManejadorErrores.class)
@DisplayName("EnvioController")
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvioService envioService;

    private static final String JSON_VALIDO = """
            {
              "sagaId": "saga-001",
              "ordenId": 1,
              "cliente": "Eiler",
              "direccion": "Zona 3, Quetzaltenango"
            }
            """;

    @Test
    @DisplayName("POST /shipping/schedule devuelve 201 con el envio programado")
    void programarDevuelve201() throws Exception {
        when(envioService.programar(any(EnvioRequest.class)))
                .thenReturn(new EnvioResponse(envioEjemplo(EstadoEnvio.PROGRAMADO)));

        mockMvc.perform(post("/shipping/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.guia").value("GUIA-ABC12345"));
    }

    @Test
    @DisplayName("POST /shipping/schedule devuelve 503 cuando el servicio no esta disponible")
    void programarNoDisponibleDevuelve503() throws Exception {
        when(envioService.programar(any(EnvioRequest.class)))
                .thenThrow(new EnvioNoDisponibleException(
                        "Servicio de envios no disponible temporalmente"));

        mockMvc.perform(post("/shipping/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.tipo").value("INFRAESTRUCTURA"));
    }

    @Test
    @DisplayName("POST /shipping/schedule devuelve 400 si faltan campos obligatorios")
    void programarSinCamposDevuelve400() throws Exception {
        String incompleto = """
                { "cliente": "Eiler" }
                """;

        mockMvc.perform(post("/shipping/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sagaId").exists())
                .andExpect(jsonPath("$.ordenId").exists())
                .andExpect(jsonPath("$.direccion").exists());
    }

    @Test
    @DisplayName("DELETE /shipping/{id} ejecuta la compensacion")
    void cancelarEjecutaCompensacion() throws Exception {
        EntidadEnvio cancelado = envioEjemplo(EstadoEnvio.CANCELADO);
        cancelado.setFechaCancelacion(LocalDateTime.now());

        when(envioService.cancelar(1L)).thenReturn(new EnvioResponse(cancelado));

        mockMvc.perform(delete("/shipping/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.fechaCancelacion").isNotEmpty())
                .andExpect(jsonPath("$.guia").isNotEmpty());
    }

    @Test
    @DisplayName("DELETE /shipping/{id} devuelve 404 si el envio no existe")
    void cancelarInexistenteDevuelve404() throws Exception {
        when(envioService.cancelar(eq(99L)))
                .thenThrow(new NoSuchElementException("Envio no encontrado: 99"));

        mockMvc.perform(delete("/shipping/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /shipping/{id} devuelve el envio")
    void obtenerDevuelveEnvio() throws Exception {
        when(envioService.obtenerPorId(1L))
                .thenReturn(new EnvioResponse(envioEjemplo(EstadoEnvio.PROGRAMADO)));

        mockMvc.perform(get("/shipping/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Eiler"));
    }

    private EntidadEnvio envioEjemplo(EstadoEnvio estado) {
        EntidadEnvio e = new EntidadEnvio();
        e.setId(1L);
        e.setSagaId("saga-001");
        e.setOrdenId(1L);
        e.setCliente("Eiler");
        e.setDireccion("Zona 3, Quetzaltenango");
        e.setGuia("GUIA-ABC12345");
        e.setEstado(estado);
        e.setFechaProgramada(LocalDateTime.now().plusDays(3));
        return e;
    }
}
