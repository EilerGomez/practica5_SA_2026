/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.dtoSimulacion;

/**
 *
 * @author eiler
 */

import lombok.Data;

@Data
public class SimulacionRequest {

    private Boolean falloActivo;
    private Long retrasoMs;
}
