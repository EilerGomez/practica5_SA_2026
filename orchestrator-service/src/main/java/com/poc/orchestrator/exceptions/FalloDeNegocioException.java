/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.exceptions;

/**
 *
 * @author eiler
 */
/**
 * Representa un rechazo legitimo de negocio, no una caida del servicio.
 * Ejemplo: no hay stock suficiente.
 *
 * El Circuit Breaker debe IGNORAR estas excepciones: el servicio
 * responde correctamente, solo que la operacion no procede.
 */
public class FalloDeNegocioException extends RuntimeException {

    public FalloDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
