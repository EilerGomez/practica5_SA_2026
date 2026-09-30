/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  eiler
 * Created: 30 sept 2026
 */

CREATE TABLE productos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL UNIQUE,
    stock_total     INTEGER      NOT NULL,
    stock_reservado INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT chk_stock_no_negativo CHECK (stock_total >= 0),
    CONSTRAINT chk_reservado_valido  CHECK (stock_reservado >= 0 AND stock_reservado <= stock_total)
);

CREATE TABLE reservas (
    id            BIGSERIAL PRIMARY KEY,
    saga_id       VARCHAR(60)  NOT NULL,
    orden_id      BIGINT       NOT NULL,
    producto      VARCHAR(120) NOT NULL,
    cantidad      INTEGER      NOT NULL,
    estado        VARCHAR(20)  NOT NULL,
    fecha_reserva TIMESTAMP    NOT NULL,
    fecha_liberacion TIMESTAMP
);

CREATE INDEX idx_reservas_saga ON reservas (saga_id);

-- Stock inicial para las pruebas
INSERT INTO productos (nombre, stock_total, stock_reservado) VALUES
    ('Laptop',   10, 0),
    ('Mouse',    50, 0),
    ('Teclado',  25, 0),
    ('Monitor',   3, 0);