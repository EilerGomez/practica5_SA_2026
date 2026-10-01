/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.dtoClientes;

/**
 *
 * @author eiler
 */
import lombok.Data;

@Data
public class ReservaClienteResponse {
    private Long id;
    private String sagaId;
    private String estado;
}
