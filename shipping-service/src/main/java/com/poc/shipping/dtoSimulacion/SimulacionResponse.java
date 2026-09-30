/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.dtoSimulacion;

/**
 *
 * @author eiler
 */

import lombok.Value;

@Value
public class SimulacionResponse {

    private boolean falloActivo;
    private long retrasoMs;
}
