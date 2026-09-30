/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.dtoPago;

/**
 *
 * @author eiler
 */

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoRequest {

    @NotBlank(message = "El sagaId es obligatorio")
    private String sagaId;

    @NotNull(message = "El ordenId es obligatorio")
    private Long ordenId;

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    @NotNull
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;
}
