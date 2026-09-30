/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.payment.repositories.payment;

/**
 *
 * @author eiler
 */
import com.poc.payment.models.payment.EntidadPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<EntidadPago, Long> {

    Optional<EntidadPago> findBySagaId(String sagaId);

    List<EntidadPago> findAllByOrderByIdDesc();
}
