/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.payment.services.payment;

/**
 *
 * @author eiler
 */

import com.poc.payment.dtoPago.PagoRequest;
import com.poc.payment.dtoPago.PagoResponse;

import java.util.List;

public interface PagoService {

    PagoResponse cobrar(PagoRequest request);

    PagoResponse reembolsar(Long id);

    PagoResponse obtenerPorId(Long id);

    List<PagoResponse> listar();
}
