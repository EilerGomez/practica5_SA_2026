/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.order.dtoOrden;

/**
 *
 * @author eiler
 */

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrdenRequest {

    @NotBlank(message = "El sagaId es obligatorio")
    private String sagaId;

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    @NotBlank(message = "El producto es obligatorio")
    private String producto;

    @NotNull @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull @DecimalMin(value = "0.01", message = "El total debe ser mayor a cero")
    private BigDecimal total;
}