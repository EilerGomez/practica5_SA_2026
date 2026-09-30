/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  eiler
 * Created: 29 sept 2026
 */

CREATE TABLE ordenes (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         VARCHAR(60)  NOT NULL,
    cliente         VARCHAR(120) NOT NULL,
    producto        VARCHAR(120) NOT NULL,
    cantidad        INTEGER      NOT NULL,
    total           NUMERIC(12,2) NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    fecha_creacion  TIMESTAMP    NOT NULL,
    fecha_cancelacion TIMESTAMP
);

CREATE INDEX idx_ordenes_saga ON ordenes (saga_id);