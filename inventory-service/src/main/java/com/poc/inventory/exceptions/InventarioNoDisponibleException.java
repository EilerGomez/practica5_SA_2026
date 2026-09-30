/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.exceptions;

/**
 *
 * @author eiler
 */
public class InventarioNoDisponibleException extends RuntimeException {

    public InventarioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
