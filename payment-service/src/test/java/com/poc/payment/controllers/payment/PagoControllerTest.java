/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.controllers.payment;

/**
 *
 * @author eiler
 */

import com.poc.payment.controllers.advice.ManejadorErrores;
import com.poc.payment.dtoPago.PagoRequest;
import com.poc.payment.dtoPago.PagoResponse;
import com.poc.payment.exceptions.PagoRechazadoException;
import com.poc.payment.models.payment.EntidadPago;
import com.poc.payment.models.payment.EstadoPago;
import com.poc.payment.services.payment.PagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagoController.class)
@Import(ManejadorErrores.class)
@DisplayName("PagoController")
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService pagoService;

    private static final String JSON_VALIDO = """
            {
              "sagaId": "saga-001",
              "ordenId": 1,
              "cliente": "Eiler",
              "monto": 8500.00
            }
            """;

    @Test
    @DisplayName("POST /payments devuelve 201 con el pago cobrado")
    void cobrarDevuelve201() throws Exception {
        when(pagoService.cobrar(any(PagoRequest.class)))
                .thenReturn(new PagoResponse(pagoEjemplo(EstadoPago.COBRADO)));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("COBRADO"))
                .andExpect(jsonPath("$.referencia").value("REF-ABC12345"));
    }

    @Test
    @DisplayName("POST /payments devuelve 503 cuando el pago es rechazado")
    void cobrarRechazadoDevuelve503() throws Exception {
        when(pagoService.cobrar(any(PagoRequest.class)))
                .thenThrow(new PagoRechazadoException("Pago rechazado por el procesador"));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /payments devuelve 400 si faltan campos obligatorios")
    void cobrarSinCamposDevuelve400() throws Exception {
        String incompleto = """
                { "cliente": "Eiler" }
                """;

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sagaId").exists())
                .andExpect(jsonPath("$.ordenId").exists());
    }

    @Test
    @DisplayName("POST /payments rechaza monto en cero")
    void cobrarConMontoInvalido() throws Exception {
        String montoCero = """
                {
                  "sagaId": "saga-001",
                  "ordenId": 1,
                  "cliente": "Eiler",
                  "monto": 0
                }
                """;

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(montoCero))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.monto").exists());
    }

    @Test
    @DisplayName("POST /payments/{id}/refund ejecuta la compensacion")
    void reembolsarEjecutaCompensacion() throws Exception {
        EntidadPago reembolsado = pagoEjemplo(EstadoPago.REEMBOLSADO);
        reembolsado.setFechaReembolso(LocalDateTime.now());

        when(pagoService.reembolsar(1L)).thenReturn(new PagoResponse(reembolsado));

        mockMvc.perform(post("/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REEMBOLSADO"))
                .andExpect(jsonPath("$.fechaReembolso").isNotEmpty())
                .andExpect(jsonPath("$.fechaCobro").isNotEmpty());
    }

    @Test
    @DisplayName("POST /payments/{id}/refund devuelve 404 si el pago no existe")
    void reembolsarInexistenteDevuelve404() throws Exception {
        when(pagoService.reembolsar(eq(99L)))
                .thenThrow(new NoSuchElementException("Pago no encontrado: 99"));

        mockMvc.perform(post("/payments/99/refund"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /payments/{id} devuelve el pago")
    void obtenerDevuelvePago() throws Exception {
        when(pagoService.obtenerPorId(1L))
                .thenReturn(new PagoResponse(pagoEjemplo(EstadoPago.COBRADO)));

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Eiler"));
    }

    private EntidadPago pagoEjemplo(EstadoPago estado) {
        EntidadPago e = new EntidadPago();
        e.setId(1L);
        e.setSagaId("saga-001");
        e.setOrdenId(1L);
        e.setCliente("Eiler");
        e.setMonto(new BigDecimal("8500.00"));
        e.setEstado(estado);
        e.setReferencia("REF-ABC12345");
        e.setFechaCobro(LocalDateTime.now());
        return e;
    }
}
