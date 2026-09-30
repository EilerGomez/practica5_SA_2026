/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.shipping.repositories.shipping;

/**
 *
 * @author eiler
 */
import com.poc.shipping.models.shipping.EntidadEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<EntidadEnvio, Long> {

    Optional<EntidadEnvio> findBySagaId(String sagaId);

    List<EntidadEnvio> findAllByOrderByIdDesc();
}
