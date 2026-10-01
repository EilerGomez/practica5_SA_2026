/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  eiler
 * Created: 30 sept 2026
 */

CREATE TABLE sagas (
    id            BIGSERIAL PRIMARY KEY,
    saga_id       VARCHAR(60)   NOT NULL UNIQUE,
    cliente       VARCHAR(120)  NOT NULL,
    producto      VARCHAR(120)  NOT NULL,
    cantidad      INTEGER       NOT NULL,
    total         NUMERIC(12,2) NOT NULL,
    direccion     VARCHAR(255)  NOT NULL,
    estado        VARCHAR(30)   NOT NULL,
    motivo_fallo  VARCHAR(500),
    orden_id      BIGINT,
    pago_id       BIGINT,
    reserva_id    BIGINT,
    envio_id      BIGINT,
    fecha_inicio  TIMESTAMP     NOT NULL,
    fecha_fin     TIMESTAMP
);

CREATE INDEX idx_sagas_saga_id ON sagas (saga_id);

CREATE TABLE saga_pasos (
    id             BIGSERIAL PRIMARY KEY,
    saga_id        VARCHAR(60) NOT NULL,
    paso           VARCHAR(40) NOT NULL,
    tipo           VARCHAR(20) NOT NULL,
    estado         VARCHAR(20) NOT NULL,
    detalle        VARCHAR(500),
    recurso_id     BIGINT,
    fecha_ejecucion TIMESTAMP  NOT NULL
);

CREATE INDEX idx_saga_pasos_saga ON saga_pasos (saga_id);