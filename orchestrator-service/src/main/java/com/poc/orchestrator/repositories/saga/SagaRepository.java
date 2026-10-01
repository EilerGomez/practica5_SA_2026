/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.repositories.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.models.saga.EntidadSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SagaRepository extends JpaRepository<EntidadSaga, Long> {

    Optional<EntidadSaga> findBySagaId(String sagaId);

    List<EntidadSaga> findAllByOrderByIdDesc();
}
