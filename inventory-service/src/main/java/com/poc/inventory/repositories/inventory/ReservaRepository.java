/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.repositories.inventory;

/**
 *
 * @author eiler
 */

import com.poc.inventory.models.inventory.EntidadReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<EntidadReserva, Long> {

    Optional<EntidadReserva> findBySagaId(String sagaId);

    List<EntidadReserva> findAllByOrderByIdDesc();
}
