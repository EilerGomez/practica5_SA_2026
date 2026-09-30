/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  eiler
 * Created: 30 sept 2026
 */

CREATE TABLE envios (
    id                BIGSERIAL PRIMARY KEY,
    saga_id           VARCHAR(60)  NOT NULL,
    orden_id          BIGINT       NOT NULL,
    cliente           VARCHAR(120) NOT NULL,
    direccion         VARCHAR(255) NOT NULL,
    guia              VARCHAR(80)  NOT NULL,
    estado            VARCHAR(20)  NOT NULL,
    fecha_programada  TIMESTAMP    NOT NULL,
    fecha_cancelacion TIMESTAMP
);

CREATE INDEX idx_envios_saga ON envios (saga_id);