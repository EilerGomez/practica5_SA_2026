/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.payment.services.simulacion;

/**
 *
 * @author eiler
 */

import com.poc.payment.dtoSimulacion.SimulacionRequest;
import com.poc.payment.dtoSimulacion.SimulacionResponse;

public interface SimulacionService {

    SimulacionResponse configurar(SimulacionRequest request);

    SimulacionResponse estado();

    boolean debeFallar();

    long retrasoMs();
}
