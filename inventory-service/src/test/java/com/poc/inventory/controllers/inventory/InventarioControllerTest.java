/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.controllers.inventory;

/**
 *
 * @author eiler
 */
import com.poc.inventory.controllers.advice.ManejadorErrores;
import com.poc.inventory.dtoInventario.ProductoResponse;
import com.poc.inventory.dtoInventario.ReservaRequest;
import com.poc.inventory.dtoInventario.ReservaResponse;
import com.poc.inventory.exceptions.InventarioNoDisponibleException;
import com.poc.inventory.exceptions.StockInsuficienteException;
import com.poc.inventory.models.inventory.EntidadProducto;
import com.poc.inventory.models.inventory.EntidadReserva;
import com.poc.inventory.models.inventory.EstadoReserva;
import com.poc.inventory.services.inventory.InventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventarioController.class)
@Import(ManejadorErrores.class)
@DisplayName("InventarioController")
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventarioService inventarioService;

    private static final String JSON_VALIDO = """
            {
              "sagaId": "saga-001",
              "ordenId": 1,
              "producto": "Laptop",
              "cantidad": 2
            }
            """;

    @Test
    @DisplayName("POST /inventory/reserve devuelve 201 con la reserva creada")
    void reservarDevuelve201() throws Exception {
        when(inventarioService.reservar(any(ReservaRequest.class)))
                .thenReturn(new ReservaResponse(reservaEjemplo(EstadoReserva.RESERVADO)));

        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("RESERVADO"))
                .andExpect(jsonPath("$.cantidad").value(2));
    }

    @Test
    @DisplayName("POST /inventory/reserve devuelve 409 por fallo de NEGOCIO")
    void reservarSinStockDevuelve409() throws Exception {
        when(inventarioService.reservar(any(ReservaRequest.class)))
                .thenThrow(new StockInsuficienteException(
                        "Stock insuficiente para Monitor. Solicitado: 5, disponible: 3"));

        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.tipo").value("NEGOCIO"));
    }

    @Test
    @DisplayName("POST /inventory/reserve devuelve 503 por fallo de INFRAESTRUCTURA")
    void reservarNoDisponibleDevuelve503() throws Exception {
        when(inventarioService.reservar(any(ReservaRequest.class)))
                .thenThrow(new InventarioNoDisponibleException(
                        "Servicio de inventario no disponible temporalmente"));

        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.tipo").value("INFRAESTRUCTURA"));
    }

    @Test
    @DisplayName("POST /inventory/reserve devuelve 400 si faltan campos obligatorios")
    void reservarSinCamposDevuelve400() throws Exception {
        String incompleto = """
                { "producto": "Laptop" }
                """;

        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sagaId").exists())
                .andExpect(jsonPath("$.ordenId").exists())
                .andExpect(jsonPath("$.cantidad").exists());
    }

    @Test
    @DisplayName("POST /inventory/reserve rechaza cantidad menor a 1")
    void reservarConCantidadInvalida() throws Exception {
        String cantidadCero = """
                {
                  "sagaId": "saga-001",
                  "ordenId": 1,
                  "producto": "Laptop",
                  "cantidad": 0
                }
                """;

        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cantidadCero))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").exists());
    }

    @Test
    @DisplayName("POST /inventory/release/{id} ejecuta la compensacion")
    void liberarEjecutaCompensacion() throws Exception {
        EntidadReserva liberada = reservaEjemplo(EstadoReserva.LIBERADO);
        liberada.setFechaLiberacion(LocalDateTime.now());

        when(inventarioService.liberar(1L)).thenReturn(new ReservaResponse(liberada));

        mockMvc.perform(post("/inventory/release/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIBERADO"))
                .andExpect(jsonPath("$.fechaLiberacion").isNotEmpty())
                .andExpect(jsonPath("$.fechaReserva").isNotEmpty());
    }

    @Test
    @DisplayName("POST /inventory/release/{id} devuelve 404 si la reserva no existe")
    void liberarInexistenteDevuelve404() throws Exception {
        when(inventarioService.liberar(eq(99L)))
                .thenThrow(new NoSuchElementException("Reserva no encontrada: 99"));

        mockMvc.perform(post("/inventory/release/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /inventory/products devuelve el stock con disponibilidad calculada")
    void listarProductosDevuelveStock() throws Exception {
        when(inventarioService.listarProductos())
                .thenReturn(List.of(
                        new ProductoResponse(producto("Laptop", 10, 3)),
                        new ProductoResponse(producto("Monitor", 3, 3))));

        mockMvc.perform(get("/inventory/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Laptop"))
                .andExpect(jsonPath("$[0].stockDisponible").value(7))
                .andExpect(jsonPath("$[1].stockDisponible").value(0));
    }

    @Test
    @DisplayName("GET /inventory/reservations/{id} devuelve la reserva")
    void obtenerReservaDevuelveDatos() throws Exception {
        when(inventarioService.obtenerReserva(1L))
                .thenReturn(new ReservaResponse(reservaEjemplo(EstadoReserva.RESERVADO)));

        mockMvc.perform(get("/inventory/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.producto").value("Laptop"));
    }

    private EntidadReserva reservaEjemplo(EstadoReserva estado) {
        EntidadReserva r = new EntidadReserva();
        r.setId(1L);
        r.setSagaId("saga-001");
        r.setOrdenId(1L);
        r.setProducto("Laptop");
        r.setCantidad(2);
        r.setEstado(estado);
        r.setFechaReserva(LocalDateTime.now());
        return r;
    }

    private EntidadProducto producto(String nombre, int total, int reservado) {
        EntidadProducto p = new EntidadProducto();
        p.setId(1L);
        p.setNombre(nombre);
        p.setStockTotal(total);
        p.setStockReservado(reservado);
        return p;
    }
}
