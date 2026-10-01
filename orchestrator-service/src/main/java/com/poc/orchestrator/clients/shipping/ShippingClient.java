/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.clients.shipping;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.config.ServiciosProperties;
import com.poc.orchestrator.dtoClientes.EnvioClienteResponse;
import com.poc.orchestrator.exceptions.PasoFallidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class ShippingClient {

    private final RestClient restClient;
    private final ServiciosProperties servicios;

    public ShippingClient(RestClient restClient, ServiciosProperties servicios) {
        this.restClient = restClient;
        this.servicios = servicios;
    }

    /** Sin circuit breaker: punto de comparacion para la demo. */
    public EnvioClienteResponse programar(String sagaId, Long ordenId,
                                          String cliente, String direccion) {
        log.debug("Llamando a shipping-service: saga={}", sagaId);

        try {
            return restClient.post()
                    .uri(servicios.getShipping() + "/shipping/schedule")
                    .body(Map.of(
                            "sagaId", sagaId,
                            "ordenId", ordenId,
                            "cliente", cliente,
                            "direccion", direccion))
                    .retrieve()
                    .body(EnvioClienteResponse.class);
        } catch (Exception e) {
            log.error("Fallo al programar el envio saga={}: {}", sagaId, e.getMessage());
            throw new PasoFallidoException("ENVIO",
                    "Error al programar el envio: " + e.getMessage(), false);
        }
    }

    /** Transaccion compensatoria. */
    public void cancelar(Long envioId) {
        log.warn("COMPENSANDO: cancelando envio id={}", envioId);

        restClient.delete()
                .uri(servicios.getShipping() + "/shipping/" + envioId)
                .retrieve()
                .toBodilessEntity();
    }
}
