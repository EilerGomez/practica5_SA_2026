/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  eiler
 * Created: 29 sept 2026
 */

CREATE TABLE pagos (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         VARCHAR(60)   NOT NULL,
    orden_id        BIGINT        NOT NULL,
    cliente         VARCHAR(120)  NOT NULL,
    monto           NUMERIC(12,2) NOT NULL,
    estado          VARCHAR(20)   NOT NULL,
    referencia      VARCHAR(80)   NOT NULL,
    fecha_cobro     TIMESTAMP     NOT NULL,
    fecha_reembolso TIMESTAMP
);

CREATE INDEX idx_pagos_saga ON pagos (saga_id);
CREATE INDEX idx_pagos_orden ON pagos (orden_id);