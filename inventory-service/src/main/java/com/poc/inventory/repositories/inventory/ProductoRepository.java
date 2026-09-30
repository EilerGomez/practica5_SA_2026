/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.repositories.inventory;

/**
 *
 * @author eiler
 */
import com.poc.inventory.models.inventory.EntidadProducto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<EntidadProducto, Long> {

    Optional<EntidadProducto> findByNombre(String nombre);

    /**
     * Bloqueo pesimista para evitar que dos sagas concurrentes
     * reserven la misma unidad de stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM EntidadProducto p WHERE p.nombre = :nombre")
    Optional<EntidadProducto> findByNombreConBloqueo(@Param("nombre") String nombre);

    List<EntidadProducto> findAllByOrderByNombreAsc();
}
