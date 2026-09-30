/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.services.simulacion;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoSimulacion.SimulacionRequest;
import com.poc.inventory.dtoSimulacion.SimulacionResponse;

public interface SimulacionService {

    SimulacionResponse configurar(SimulacionRequest request);

    SimulacionResponse estado();

    boolean debeFallar();

    long retrasoMs();
}
