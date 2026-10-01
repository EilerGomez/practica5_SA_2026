/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.controllers.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.controllers.advice.ManejadorErrores;
import com.poc.orchestrator.dtoSaga.CompraRequest;
import com.poc.orchestrator.dtoSaga.SagaResponse;
import com.poc.orchestrator.models.saga.EntidadSaga;
import com.poc.orchestrator.models.saga.EstadoSaga;
import com.poc.orchestrator.services.saga.SagaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SagaController.class)
@Import(ManejadorErrores.class)
@DisplayName("SagaController")
class SagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SagaService sagaService;

    private static final String JSON_VALIDO = """
            {
              "cliente": "Eiler",
              "producto": "Laptop",
              "cantidad": 1,
              "total": 8500.00,
              "direccion": "Zona 3, Quetzaltenango"
            }
            """;

    @Test
    @DisplayName("POST /saga/compra devuelve 200 con la saga COMPLETADA")
    void compraExitosaDevuelve200() throws Exception {
        when(sagaService.ejecutarCompra(any(CompraRequest.class)))
                .thenReturn(new SagaResponse(sagaEjemplo(EstadoSaga.COMPLETADA)));

        mockMvc.perform(post("/saga/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.sagaId").value("SAGA-TEST1234"))
                .andExpect(jsonPath("$.motivoFallo").doesNotExist());
    }

    @Test
    @DisplayName("POST /saga/compra devuelve 200 con la saga COMPENSADA cuando algo falla")
    void compraCompensadaDevuelve200() throws Exception {
        EntidadSaga compensada = sagaEjemplo(EstadoSaga.COMPENSADA);
        compensada.setMotivoFallo("Stock insuficiente para Monitor");

        when(sagaService.ejecutarCompra(any(CompraRequest.class)))
                .thenReturn(new SagaResponse(compensada));

        mockMvc.perform(post("/saga/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPENSADA"))
                .andExpect(jsonPath("$.motivoFallo").exists());
    }

    @Test
    @DisplayName("POST /saga/compra devuelve 400 si faltan campos obligatorios")
    void compraSinCamposDevuelve400() throws Exception {
        String incompleto = """
                { "cliente": "Eiler" }
                """;

        mockMvc.perform(post("/saga/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.producto").exists())
                .andExpect(jsonPath("$.cantidad").exists())
                .andExpect(jsonPath("$.direccion").exists());
    }

    @Test
    @DisplayName("POST /saga/compra rechaza cantidad menor a 1")
    void compraConCantidadInvalida() throws Exception {
        String cantidadCero = """
                {
                  "cliente": "Eiler",
                  "producto": "Laptop",
                  "cantidad": 0,
                  "total": 8500.00,
                  "direccion": "Zona 3"
                }
                """;

        mockMvc.perform(post("/saga/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cantidadCero))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").exists());
    }

    @Test
    @DisplayName("GET /saga/{sagaId} devuelve 404 si la saga no existe")
    void detalleInexistenteDevuelve404() throws Exception {
        when(sagaService.obtenerPorSagaId(eq("SAGA-NOEXISTE")))
                .thenThrow(new NoSuchElementException("Saga no encontrada: SAGA-NOEXISTE"));

        mockMvc.perform(get("/saga/SAGA-NOEXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    private EntidadSaga sagaEjemplo(EstadoSaga estado) {
        EntidadSaga s = new EntidadSaga();
        s.setId(1L);
        s.setSagaId("SAGA-TEST1234");
        s.setCliente("Eiler");
        s.setProducto("Laptop");
        s.setCantidad(1);
        s.setTotal(new BigDecimal("8500.00"));
        s.setDireccion("Zona 3, Quetzaltenango");
        s.setEstado(estado);
        s.setOrdenId(10L);
        s.setPagoId(20L);
        s.setFechaInicio(LocalDateTime.now());
        s.setFechaFin(LocalDateTime.now());
        return s;
    }
}
