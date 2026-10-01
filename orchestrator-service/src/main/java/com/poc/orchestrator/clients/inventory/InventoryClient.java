/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.clients.inventory;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.config.ServiciosProperties;
import com.poc.orchestrator.dtoClientes.ReservaClienteResponse;
import com.poc.orchestrator.exceptions.FalloDeNegocioException;
import com.poc.orchestrator.exceptions.PasoFallidoException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class InventoryClient {

    private final RestClient restClient;
    private final ServiciosProperties servicios;

    public InventoryClient(RestClient restClient, ServiciosProperties servicios) {
        this.restClient = restClient;
        this.servicios = servicios;
    }

    /**
     * CIRCUIT BREAKER 2: protege las llamadas hacia inventory-service.
     *
     * El 409 (stock insuficiente) se lanza como FalloDeNegocioException,
     * que esta configurada para ser IGNORADA por el circuito. El servicio
     * esta sano: simplemente no hay producto.
     */
    @CircuitBreaker(name = "inventory", fallbackMethod = "reservarFallback")
    public ReservaClienteResponse reservar(String sagaId, Long ordenId,
                                           String producto, Integer cantidad) {
        log.debug("Llamando a inventory-service: saga={} producto={} cantidad={}",
                sagaId, producto, cantidad);

        return restClient.post()
                .uri(servicios.getInventory() + "/inventory/reserve")
                .body(Map.of(
                        "sagaId", sagaId,
                        "ordenId", ordenId,
                        "producto", producto,
                        "cantidad", cantidad))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.warn("Fallo de NEGOCIO en inventory: stock insuficiente para {}", producto);
                    throw new FalloDeNegocioException(
                            "Stock insuficiente para " + producto);
                })
                .body(ReservaClienteResponse.class);
    }

    private ReservaClienteResponse reservarFallback(String sagaId, Long ordenId,
                                                    String producto, Integer cantidad,
                                                    Throwable t) {
        // Fallo de negocio: el servicio esta sano, la saga debe compensar
        if (t instanceof FalloDeNegocioException) {
            throw new PasoFallidoException("INVENTARIO", t.getMessage(), true);
        }

        if (t instanceof CallNotPermittedException) {
            log.error("CIRCUITO ABIERTO hacia inventory. Llamada rechazada sin intentar. saga={}", sagaId);
            throw new PasoFallidoException("INVENTARIO",
                    "Circuito abierto: inventory-service no esta disponible", false);
        }

        log.error("Fallo de INFRAESTRUCTURA al reservar saga={}: {}", sagaId, t.getMessage());
        throw new PasoFallidoException("INVENTARIO",
                "Error al reservar inventario: " + t.getMessage(), false);
    }

    /** Transaccion compensatoria. Sin circuit breaker: debe intentarse siempre. */
    public void liberar(Long reservaId) {
        log.warn("COMPENSANDO: liberando reserva id={}", reservaId);

        restClient.post()
                .uri(servicios.getInventory() + "/inventory/release/" + reservaId)
                .retrieve()
                .toBodilessEntity();
    }
}
