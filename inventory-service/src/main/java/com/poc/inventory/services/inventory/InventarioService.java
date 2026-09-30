/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.poc.inventory.services.inventory;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoInventario.ProductoResponse;
import com.poc.inventory.dtoInventario.ReservaRequest;
import com.poc.inventory.dtoInventario.ReservaResponse;

import java.util.List;

public interface InventarioService {

    ReservaResponse reservar(ReservaRequest request);

    ReservaResponse liberar(Long id);

    ReservaResponse obtenerReserva(Long id);

    List<ReservaResponse> listarReservas();

    List<ProductoResponse> listarProductos();
}
