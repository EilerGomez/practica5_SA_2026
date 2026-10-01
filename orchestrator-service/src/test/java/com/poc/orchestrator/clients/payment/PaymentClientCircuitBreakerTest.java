/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.clients.payment;

/**
 *
 * @author eiler
 */
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el comportamiento de la maquina de estados del Circuit Breaker
 * con la misma configuracion que usa el orquestador.
 */
@DisplayName("Circuit Breaker: maquina de estados")
class PaymentClientCircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(500))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        circuitBreaker = CircuitBreakerRegistry.of(config).circuitBreaker("payment-test");
    }

    @Test
    @DisplayName("Arranca en estado CLOSED")
    void arrancaEnClosed() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("No abre antes de alcanzar el minimo de llamadas")
    void noAbreAntesDelMinimo() {
        // 4 fallos: por debajo de minimumNumberOfCalls=5
        for (int i = 0; i < 4; i++) {
            ejecutarFallo();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Pasa a OPEN cuando la tasa de fallos alcanza el umbral")
    void abreAlAlcanzarElUmbral() {
        for (int i = 0; i < 5; i++) {
            ejecutarFallo();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(100.0f);
    }

    @Test
    @DisplayName("No abre si la tasa de fallos queda por debajo del umbral")
    void noAbreConTasaBaja() {
        // 2 fallos y 4 exitos = 33% de fallos, por debajo del 50%
        ejecutarFallo();
        ejecutarFallo();
        for (int i = 0; i < 4; i++) {
            ejecutarExito();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("En estado OPEN rechaza las llamadas sin ejecutarlas")
    void enOpenRechazaSinEjecutar() {
        for (int i = 0; i < 5; i++) {
            ejecutarFallo();
        }

        long llamadasAntes = circuitBreaker.getMetrics().getNumberOfFailedCalls();

        assertThatThrownBy(() ->
                circuitBreaker.executeSupplier(() -> "no deberia ejecutarse"))
                .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);

        // El contador no se movio: la llamada nunca llego al servicio
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                .isEqualTo(llamadasAntes);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls())
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Transita de OPEN a HALF_OPEN tras el tiempo de espera")
    void transitaAHalfOpen() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            ejecutarFallo();
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(600); // waitDurationInOpenState = 500ms

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("De HALF_OPEN vuelve a CLOSED si las llamadas de prueba tienen exito")
    void deHalfOpenVuelveAClosed() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            ejecutarFallo();
        }
        Thread.sleep(600);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Las 3 llamadas de prueba permitidas
        for (int i = 0; i < 3; i++) {
            ejecutarExito();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("De HALF_OPEN vuelve a OPEN si las llamadas de prueba fallan")
    void deHalfOpenVuelveAOpen() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            ejecutarFallo();
        }
        Thread.sleep(600);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        for (int i = 0; i < 3; i++) {
            ejecutarFallo();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Ignora las excepciones configuradas como fallos de negocio")
    void ignoraExcepcionesDeNegocio() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry.of(config).circuitBreaker("negocio-test");

        // 10 "fallos de negocio" ignorados
        for (int i = 0; i < 10; i++) {
            try {
                cb.executeSupplier(() -> {
                    throw new IllegalArgumentException("fallo de negocio");
                });
            } catch (Exception ignored) { }
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    // ==================== HELPERS ====================

    private void ejecutarFallo() {
        try {
            circuitBreaker.executeSupplier(fallo());
        } catch (Exception ignored) { }
    }

    private void ejecutarExito() {
        try {
            circuitBreaker.executeSupplier(() -> "ok");
        } catch (Exception ignored) { }
    }

    private Supplier<String> fallo() {
        return () -> {
            throw new RuntimeException("servicio caido");
        };
    }
}
