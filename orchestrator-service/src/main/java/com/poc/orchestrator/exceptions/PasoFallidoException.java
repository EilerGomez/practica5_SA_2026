/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.exceptions;

/**
 *
 * @author eiler
 */
import lombok.Getter;

@Getter
public class PasoFallidoException extends RuntimeException {

    private final String paso;
    private final boolean falloDeNegocio;

    public PasoFallidoException(String paso, String mensaje, boolean falloDeNegocio) {
        super(mensaje);
        this.paso = paso;
        this.falloDeNegocio = falloDeNegocio;
    }
}
