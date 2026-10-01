/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.clients.order;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.config.ServiciosProperties;
import com.poc.orchestrator.dtoClientes.OrdenClienteResponse;
import com.poc.orchestrator.exceptions.PasoFallidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class OrderClient {

    private final RestClient restClient;
    private final ServiciosProperties servicios;

    public OrderClient(RestClient restClient, ServiciosProperties servicios) {
        this.restClient = restClient;
        this.servicios = servicios;
    }

    /** Sin circuit breaker: punto de comparacion para la demo. */
    public OrdenClienteResponse crear(String sagaId, String cliente, String producto,
                                      Integer cantidad, BigDecimal total) {
        log.debug("Llamando a order-service: saga={}", sagaId);

        try {
            return restClient.post()
                    .uri(servicios.getOrder() + "/orders")
                    .body(Map.of(
                            "sagaId", sagaId,
                            "cliente", cliente,
                            "producto", producto,
                            "cantidad", cantidad,
                            "total", total))
                    .retrieve()
                    .body(OrdenClienteResponse.class);
        } catch (Exception e) {
            log.error("Fallo al crear la orden saga={}: {}", sagaId, e.getMessage());
            throw new PasoFallidoException("ORDEN",
                    "Error al crear la orden: " + e.getMessage(), false);
        }
    }

    /** Transaccion compensatoria. */
    public void cancelar(Long ordenId) {
        log.warn("COMPENSANDO: cancelando orden id={}", ordenId);

        restClient.delete()
                .uri(servicios.getOrder() + "/orders/" + ordenId)
                .retrieve()
                .toBodilessEntity();
    }
}
