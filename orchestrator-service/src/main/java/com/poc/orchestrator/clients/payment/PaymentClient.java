/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.clients.payment;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.config.ServiciosProperties;
import com.poc.orchestrator.dtoClientes.PagoClienteResponse;
import com.poc.orchestrator.exceptions.PasoFallidoException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class PaymentClient {

    private final RestClient restClient;
    private final ServiciosProperties servicios;

    public PaymentClient(RestClient restClient, ServiciosProperties servicios) {
        this.restClient = restClient;
        this.servicios = servicios;
    }

    /**
     * CIRCUIT BREAKER 1: protege las llamadas hacia payment-service.
     * Si la tasa de fallos supera el 50% en las ultimas 10 llamadas,
     * el circuito se abre y deja de intentar contactar al servicio.
     */
    @CircuitBreaker(name = "payment", fallbackMethod = "cobrarFallback")
    public PagoClienteResponse cobrar(String sagaId, Long ordenId,
                                      String cliente, BigDecimal monto) {
        log.debug("Llamando a payment-service: saga={} monto={}", sagaId, monto);

        return restClient.post()
                .uri(servicios.getPayment() + "/payments")
                .body(Map.of(
                        "sagaId", sagaId,
                        "ordenId", ordenId,
                        "cliente", cliente,
                        "monto", monto))
                .retrieve()
                .body(PagoClienteResponse.class);
    }

    /**
     * Se ejecuta cuando el circuito esta ABIERTO o cuando la llamada falla.
     * Convierte cualquier problema en una excepcion que la saga entiende.
     */
    private PagoClienteResponse cobrarFallback(String sagaId, Long ordenId,
                                               String cliente, BigDecimal monto,
                                               Throwable t) {
        if (t instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            log.error("CIRCUITO ABIERTO hacia payment. Llamada rechazada sin intentar. saga={}", sagaId);
            throw new PasoFallidoException("PAGO",
                    "Circuito abierto: payment-service no esta disponible", false);
        }

        log.error("Fallo al cobrar saga={}: {}", sagaId, t.getMessage());
        throw new PasoFallidoException("PAGO",
                "Error al procesar el pago: " + t.getMessage(), false);
    }

    /** Transaccion compensatoria. Sin circuit breaker: debe intentarse siempre. */
    public void reembolsar(Long pagoId) {
        log.warn("COMPENSANDO: solicitando reembolso del pago id={}", pagoId);

        restClient.post()
                .uri(servicios.getPayment() + "/payments/" + pagoId + "/refund")
                .retrieve()
                .toBodilessEntity();
    }
}
