/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.order.services.order;

/**
 *
 * @author eiler
 */

import com.poc.order.dtoOrden.OrdenRequest;
import com.poc.order.dtoOrden.OrdenResponse;

import java.util.List;

public interface OrdenService {

    OrdenResponse crear(OrdenRequest request);

    OrdenResponse cancelar(Long id);

    OrdenResponse confirmar(Long id);

    OrdenResponse obtenerPorId(Long id);

    List<OrdenResponse> listar();
}
