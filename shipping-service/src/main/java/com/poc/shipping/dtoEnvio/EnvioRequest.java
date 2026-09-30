/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.dtoEnvio;

/**
 *
 * @author eiler
 */
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnvioRequest {

    @NotBlank(message = "El sagaId es obligatorio")
    private String sagaId;

    @NotNull(message = "El ordenId es obligatorio")
    private Long ordenId;

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    @NotBlank(message = "La direccion es obligatoria")
    private String direccion;
}
