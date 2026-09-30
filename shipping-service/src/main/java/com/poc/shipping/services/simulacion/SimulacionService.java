/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.shipping.services.simulacion;

/**
 *
 * @author eiler
 */

import com.poc.shipping.dtoSimulacion.SimulacionRequest;
import com.poc.shipping.dtoSimulacion.SimulacionResponse;

public interface SimulacionService {

    SimulacionResponse configurar(SimulacionRequest request);

    SimulacionResponse estado();

    boolean debeFallar();

    long retrasoMs();
}
