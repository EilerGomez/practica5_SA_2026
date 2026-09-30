/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.order.repositories.orden;

import com.poc.order.models.orden.EntidadOrden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author eiler
 */


public interface OrdenRepository extends JpaRepository<EntidadOrden, Long> {

    Optional<EntidadOrden> findBySagaId(String sagaId);

    List<EntidadOrden> findAllByOrderByIdDesc();
}
