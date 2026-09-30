/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.exceptions;

/**
 *
 * @author eiler
 */
public class EnvioNoDisponibleException extends RuntimeException {

    public EnvioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
