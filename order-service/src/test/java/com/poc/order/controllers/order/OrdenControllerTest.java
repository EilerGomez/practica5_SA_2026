/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.order.controllers.order;

/**
 *
 * @author eiler
 */

import com.poc.order.dtoOrden.OrdenRequest;
import com.poc.order.dtoOrden.OrdenResponse;
import com.poc.order.models.orden.EntidadOrden;
import com.poc.order.models.orden.EstadoOrden;
import com.poc.order.services.order.OrdenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(OrdenController.class)
@DisplayName("OrdenController")
class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdenService ordenService;

    private static final String JSON_VALIDO = """
            {
              "sagaId": "saga-001",
              "cliente": "Eiler",
              "producto": "Laptop",
              "cantidad": 1,
              "total": 8500.00
            }
            """;

    @Test
    @DisplayName("POST /orders devuelve 201 con la orden creada")
    void crearDevuelve201() throws Exception {
        when(ordenService.crear(any(OrdenRequest.class)))
                .thenReturn(new OrdenResponse(ordenEjemplo(EstadoOrden.CREADA)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("CREADA"))
                .andExpect(jsonPath("$.sagaId").value("saga-001"));
    }

    @Test
    @DisplayName("POST /orders devuelve 400 si faltan campos obligatorios")
    void crearSinCamposDevuelve400() throws Exception {
        String incompleto = """
                { "cliente": "Eiler" }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sagaId").exists())
                .andExpect(jsonPath("$.producto").exists());
    }

    @Test
    @DisplayName("POST /orders rechaza cantidad menor a 1")
    void crearConCantidadInvalida() throws Exception {
        String cantidadCero = """
                {
                  "sagaId": "saga-001",
                  "cliente": "Eiler",
                  "producto": "Laptop",
                  "cantidad": 0,
                  "total": 8500.00
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cantidadCero))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").exists());
    }

    @Test
    @DisplayName("DELETE /orders/{id} ejecuta la compensacion")
    void cancelarEjecutaCompensacion() throws Exception {
        EntidadOrden cancelada = ordenEjemplo(EstadoOrden.CANCELADA);
        cancelada.setFechaCancelacion(LocalDateTime.now());

        when(ordenService.cancelar(1L)).thenReturn(new OrdenResponse(cancelada));

        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.fechaCancelacion").isNotEmpty());
    }

    @Test
    @DisplayName("DELETE /orders/{id} devuelve 404 si la orden no existe")
    void cancelarInexistenteDevuelve404() throws Exception {
        when(ordenService.cancelar(eq(99L)))
                .thenThrow(new NoSuchElementException("Orden no encontrada: 99"));

        mockMvc.perform(delete("/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /orders/{id} devuelve la orden")
    void obtenerDevuelveOrden() throws Exception {
        when(ordenService.obtenerPorId(1L))
                .thenReturn(new OrdenResponse(ordenEjemplo(EstadoOrden.CREADA)));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Eiler"));
    }

    private EntidadOrden ordenEjemplo(EstadoOrden estado) {
        EntidadOrden e = new EntidadOrden();
        e.setId(1L);
        e.setSagaId("saga-001");
        e.setCliente("Eiler");
        e.setProducto("Laptop");
        e.setCantidad(1);
        e.setTotal(new BigDecimal("8500.00"));
        e.setEstado(estado);
        e.setFechaCreacion(LocalDateTime.now());
        return e;
    }
}