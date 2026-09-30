/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.dtoInventario;

/**
 *
 * @author eiler
 */
import com.poc.inventory.models.inventory.EntidadProducto;
import lombok.Value;

@Value
public class ProductoResponse {

    private Long id;
    private String nombre;
    private Integer stockTotal;
    private Integer stockReservado;
    private Integer stockDisponible;

    public ProductoResponse(EntidadProducto ep) {
        this.id = ep.getId();
        this.nombre = ep.getNombre();
        this.stockTotal = ep.getStockTotal();
        this.stockReservado = ep.getStockReservado();
        this.stockDisponible = ep.getStockDisponible();
    }
}
