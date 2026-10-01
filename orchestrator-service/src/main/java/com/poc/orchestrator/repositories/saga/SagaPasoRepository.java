/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.repositories.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.models.saga.EntidadSagaPaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SagaPasoRepository extends JpaRepository<EntidadSagaPaso, Long> {

    List<EntidadSagaPaso> findBySagaIdOrderByIdAsc(String sagaId);
}
