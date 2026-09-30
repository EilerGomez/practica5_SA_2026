/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.services.simulacion;

/**
 *
 * @author eiler
 */

import com.poc.shipping.dtoSimulacion.SimulacionRequest;
import com.poc.shipping.dtoSimulacion.SimulacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulacionServiceImpl")
class SimulacionServiceImplTest {

    private SimulacionServiceImpl simulacionService;

    @BeforeEach
    void setUp() {
        simulacionService = new SimulacionServiceImpl(false, 0L);
    }

    @Test
    @DisplayName("Arranca con los valores iniciales inyectados")
    void arrancaConValoresIniciales() {
        SimulacionServiceImpl conFallo = new SimulacionServiceImpl(true, 500L);

        assertThat(conFallo.debeFallar()).isTrue();
        assertThat(conFallo.retrasoMs()).isEqualTo(500L);
    }

    @Test
    @DisplayName("configurar() activa el fallo")
    void configurarActivaFallo() {
        SimulacionRequest request = new SimulacionRequest();
        request.setFalloActivo(true);

        SimulacionResponse response = simulacionService.configurar(request);

        assertThat(response.isFalloActivo()).isTrue();
        assertThat(simulacionService.debeFallar()).isTrue();
    }

    @Test
    @DisplayName("configurar() actualiza el retraso")
    void configurarActualizaRetraso() {
        SimulacionRequest request = new SimulacionRequest();
        request.setRetrasoMs(2000L);

        SimulacionResponse response = simulacionService.configurar(request);

        assertThat(response.getRetrasoMs()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("configurar() ignora los campos nulos y conserva el valor anterior")
    void configurarIgnoraNulos() {
        SimulacionRequest activar = new SimulacionRequest();
        activar.setFalloActivo(true);
        activar.setRetrasoMs(1000L);
        simulacionService.configurar(activar);

        // Solo cambia el retraso, falloActivo viene nulo
        SimulacionRequest soloRetraso = new SimulacionRequest();
        soloRetraso.setRetrasoMs(0L);
        SimulacionResponse response = simulacionService.configurar(soloRetraso);

        assertThat(response.isFalloActivo()).isTrue();
        assertThat(response.getRetrasoMs()).isZero();
    }

    @Test
    @DisplayName("configurar() normaliza retrasos negativos a cero")
    void configurarNormalizaNegativos() {
        SimulacionRequest request = new SimulacionRequest();
        request.setRetrasoMs(-500L);

        SimulacionResponse response = simulacionService.configurar(request);

        assertThat(response.getRetrasoMs()).isZero();
    }

    @Test
    @DisplayName("estado() refleja la configuracion actual")
    void estadoReflejaConfiguracion() {
        SimulacionRequest request = new SimulacionRequest();
        request.setFalloActivo(true);
        request.setRetrasoMs(750L);
        simulacionService.configurar(request);

        SimulacionResponse estado = simulacionService.estado();

        assertThat(estado.isFalloActivo()).isTrue();
        assertThat(estado.getRetrasoMs()).isEqualTo(750L);
    }
}
