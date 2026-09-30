/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.shipping.services.shipping;

/**
 *
 * @author eiler
 */
import com.poc.shipping.dtoEnvio.EnvioRequest;
import com.poc.shipping.dtoEnvio.EnvioResponse;

import java.util.List;

public interface EnvioService {

    EnvioResponse programar(EnvioRequest request);

    EnvioResponse cancelar(Long id);

    EnvioResponse obtenerPorId(Long id);

    List<EnvioResponse> listar();
}
