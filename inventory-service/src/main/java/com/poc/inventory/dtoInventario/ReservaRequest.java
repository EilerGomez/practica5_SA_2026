/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.dtoInventario;

/**
 *
 * @author eiler
 */

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReservaRequest {

    @NotBlank(message = "El sagaId es obligatorio")
    private String sagaId;

    @NotNull(message = "El ordenId es obligatorio")
    private Long ordenId;

    @NotBlank(message = "El producto es obligatorio")
    private String producto;

    @NotNull
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
}
