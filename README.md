# practica5_SA_2026 — Saga Pattern vs Circuit Breaker Pattern

Prueba de concepto que implementa dos patrones fundamentales de arquitecturas de microservicios: **Saga Pattern** para transacciones distribuidas con compensaciones, y **Circuit Breaker Pattern** para resiliencia ante dependencias fallidas.

**Universidad de San Carlos de Guatemala — Centro Universitario de Occidente**
Ingeniería en Ciencias y Sistemas · Software Avanzado

---

## Tabla de contenido

- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos previos](#requisitos-previos)
- [Instalación](#instalación)
- [Ejecución](#ejecución)
- [Endpoints](#endpoints)
- [Pruebas de fallo](#pruebas-de-fallo)
- [Tests automatizados](#tests-automatizados)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Problemas frecuentes](#problemas-frecuentes)
- [Referencias](#referencias)

---

## Arquitectura

```
                    ┌──────────────────┐
                    │  Frontend React  │
                    │      :5173       │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────────────┐
                    │  Orchestrator Service    │
                    │  Saga + Circuit Breakers │
                    │         :8080            │
                    └──┬────┬──────┬────────┬──┘
                       │    │ CB   │ CB     │
              ┌────────┘    │      │        └────────┐
              │             │      │                 │
        ┌─────▼────┐  ┌─────▼───┐ ┌▼──────────┐ ┌────▼─────┐
        │  Order   │  │ Payment │ │ Inventory │ │ Shipping │
        │  :8081   │  │  :8082  │ │   :8083   │ │  :8084   │
        └─────┬────┘  └────┬────┘ └─────┬─────┘ └────┬─────┘
              │            │            │            │
        ┌─────▼────────────▼────────────▼────────────▼─────┐
        │          PostgreSQL :5432                        │
        │  saga_db · orders_db · payments_db                │
        │  inventory_db · shipping_db                       │
        └──────────────────────────────────────────────────┘
```

**Orquestación con orquestador dedicado.** El `orchestrator-service` conoce la secuencia completa de la saga y coordina a los cuatro participantes. Los participantes no se conocen entre sí.

**Database per service.** Cada microservicio tiene su propia base de datos. Ningún servicio accede a la base de otro.

**Dos Circuit Breakers**, en las llamadas del orquestador hacia Payment e Inventory. Order y Shipping quedan sin protección deliberadamente, como punto de comparación.

---

## Tecnologías

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 4.1.1 |
| Build | Maven (wrapper incluido) | 3.9+ |
| Base de datos | PostgreSQL | 16 |
| Migraciones | Flyway | incluida en Boot |
| ORM | Hibernate / Spring Data JPA | 7.4 |
| Resiliencia | Resilience4j | 2.4.0 (`resilience4j-spring-boot4`) |
| Boilerplate | Lombok | incluida en Boot |
| Frontend | React + Vite | 18 / 5 |
| Tests | JUnit 5, Mockito, AssertJ | incluidas en Boot |

> **Nota sobre Spring Boot 4:** esta versión renombró varios starters (`spring-boot-starter-web` → `spring-boot-starter-webmvc`, `spring-boot-starter-aop` → `spring-boot-starter-aspectj`) y movió las anotaciones de test (`@WebMvcTest` ahora está en `org.springframework.boot.webmvc.test.autoconfigure`). También eliminó `@MockBean` en favor de `@MockitoBean`.

---

## Requisitos previos

| Herramienta | Verificar con |
|---|---|
| JDK 21 | `java -version` |
| PostgreSQL 16 | `psql --version` |
| Node.js 18+ | `node --version` |
| npm | `npm --version` |

Maven no requiere instalación: cada servicio incluye su wrapper `mvnw`.

**Puertos necesarios:** 5173, 5432, 8080, 8081, 8082, 8083, 8084

```bash
lsof -i :5173 -i :8080 -i :8081 -i :8082 -i :8083 -i :8084
```

---

## Instalación

### 1. Clonar

```bash
git clone https://github.com/EilerGomez/practica5_SA_2026.git
cd practica5_SA_2026
```

### 2. Crear las bases de datos

```bash
psql -U postgres -c "CREATE DATABASE saga_db;"
psql -U postgres -c "CREATE DATABASE orders_db;"
psql -U postgres -c "CREATE DATABASE payments_db;"
psql -U postgres -c "CREATE DATABASE inventory_db;"
psql -U postgres -c "CREATE DATABASE shipping_db;"
```

Verificar:

```bash
psql -U postgres -c "\l" | grep _db
```

| Base de datos | Servicio | Tablas |
|---|---|---|
| `saga_db` | orchestrator-service | `sagas`, `saga_pasos` |
| `orders_db` | order-service | `ordenes` |
| `payments_db` | payment-service | `pagos` |
| `inventory_db` | inventory-service | `productos`, `reservas` |
| `shipping_db` | shipping-service | `envios` |

Las tablas se crean automáticamente con Flyway al arrancar cada servicio. **No hay que ejecutar SQL manualmente.**

### 3. Configurar la contraseña de PostgreSQL

Edita el `application.properties` de los cinco servicios y ajusta:

```properties
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD
```

### 4. Instalar dependencias del frontend

```bash
cd frontend
npm install
cd ..
```

---

## Ejecución

### Opción A — Terminales separadas (recomendado para demostrar)

Cinco terminales, una por servicio:

```bash
cd order-service       && ./mvnw spring-boot:run   # :8081
cd payment-service     && ./mvnw spring-boot:run   # :8082
cd inventory-service   && ./mvnw spring-boot:run   # :8083
cd shipping-service    && ./mvnw spring-boot:run   # :8084
cd orchestrator-service && ./mvnw spring-boot:run  # :8080
```

Y el frontend:

```bash
cd frontend && npm run dev   # :5173
```

Ver los logs de cada servicio por separado es muy útil para observar las compensaciones ejecutándose en orden inverso.

### Opción B — Script

```bash
./levantar.sh    # levanta los cinco servicios en segundo plano
./detener.sh     # los detiene y libera los puertos
```

Los logs quedan en `logs/`:

```bash
tail -f logs/orchestrator-service.log
```

### Verificar que todo está arriba

```bash
for p in 8080 8081 8082 8083 8084; do
  echo -n "puerto $p: "
  curl -s http://localhost:$p/actuator/health || echo "sin respuesta"
  echo ""
done
```

Y los circuit breakers registrados:

```bash
curl -s http://localhost:8080/actuator/circuitbreakers | python3 -m json.tool
```

Debe mostrar `payment` e `inventory` en estado `CLOSED`.

---

## Endpoints

### Orquestador (:8080)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/saga/compra` | Ejecuta la saga completa |
| GET | `/saga/{sagaId}` | Traza paso a paso de una saga |
| GET | `/saga` | Historial de sagas |
| GET | `/actuator/circuitbreakers` | Estado de los circuitos |
| GET | `/actuator/circuitbreakerevents` | Eventos de transición |

### Order Service (:8081)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/orders` | Crear orden |
| DELETE | `/orders/{id}` | **Compensación:** cancelar |
| GET | `/orders` · `/orders/{id}` | Consultar |

### Payment Service (:8082)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/payments` | Cobrar |
| POST | `/payments/{id}/refund` | **Compensación:** reembolsar |
| GET | `/payments` · `/payments/{id}` | Consultar |

### Inventory Service (:8083)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/inventory/reserve` | Reservar stock |
| POST | `/inventory/release/{id}` | **Compensación:** liberar |
| GET | `/inventory/products` | Stock disponible |
| GET | `/inventory/reservations` | Reservas |

### Shipping Service (:8084)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/shipping/schedule` | Programar envío |
| DELETE | `/shipping/{id}` | **Compensación:** cancelar |
| GET | `/shipping` · `/shipping/{id}` | Consultar |

### Simulación de fallos

Disponible en los puertos 8082, 8083 y 8084:

| Método | Ruta | Cuerpo |
|---|---|---|
| GET | `/simulacion` | — |
| POST | `/simulacion` | `{"falloActivo": true, "retrasoMs": 4000}` |

---

## Pruebas de fallo

### Flujo exitoso

```bash
curl -X POST http://localhost:8080/saga/compra \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": "Eiler",
    "producto": "Laptop",
    "cantidad": 1,
    "total": 8500.00,
    "direccion": "Zona 3, Quetzaltenango"
  }'
```

Resultado esperado: `estado: "COMPLETADA"` con los cuatro ids asignados.

---

### Prueba 1 — Compensación por fallo de negocio

El stock inicial tiene solo 3 monitores. Pedir 5 provoca un rechazo legítimo.

```bash
curl -X POST http://localhost:8080/saga/compra \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Eiler","producto":"Monitor","cantidad":5,"total":4500.00,"direccion":"Zona 1"}'
```

Resultado: `estado: "COMPENSADA"`. La traza muestra ORDEN y PAGO exitosos, INVENTARIO fallido, y luego las compensaciones de PAGO y ORDEN **en orden inverso**.

Verificar en las bases:

```bash
psql -U postgres -d orders_db -c "SELECT id, estado FROM ordenes ORDER BY id DESC LIMIT 1;"
psql -U postgres -d payments_db -c "SELECT id, estado, fecha_reembolso FROM pagos ORDER BY id DESC LIMIT 1;"
```

La orden queda en `CANCELADA` y el pago en `REEMBOLSADO`.

---

### Prueba 2 — Compensación por fallo de infraestructura

```bash
curl -X POST http://localhost:8084/simulacion \
  -H "Content-Type: application/json" -d '{"falloActivo": true}'

curl -X POST http://localhost:8080/saga/compra \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Eiler","producto":"Laptop","cantidad":2,"total":17000.00,"direccion":"Zona 1"}'

curl -X POST http://localhost:8084/simulacion \
  -H "Content-Type: application/json" -d '{"falloActivo": false}'
```

Al fallar el último paso, se compensan los tres anteriores. El stock de laptops se reserva y vuelve a liberarse.

---

### Prueba 3 — Apertura del Circuit Breaker

```bash
# Tumbar payment
curl -X POST http://localhost:8082/simulacion \
  -H "Content-Type: application/json" -d '{"falloActivo": true}'

# Lanzar compras y observar el circuito
for i in {1..8}; do
  echo "--- Compra $i ---"
  curl -s -X POST http://localhost:8080/saga/compra \
    -H "Content-Type: application/json" \
    -d '{"cliente":"Test","producto":"Mouse","cantidad":1,"total":150.00,"direccion":"Zona 1"}' \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(f\"  {d['estado']}: {d['motivoFallo']}\")"

  curl -s http://localhost:8080/actuator/circuitbreakers \
    | python3 -c "import sys,json; d=json.load(sys.stdin)['circuitBreakers']['payment']; print(f\"  circuito: {d['state']} | fallos: {d['failedCalls']}/{d['bufferedCalls']} | tasa: {d['failureRate']}\")"
  echo ""
done
```

**Comportamiento observado:**

| Compra | Tasa de fallos | Estado |
|---|---|---|
| 1 | 20% | CLOSED |
| 2 | 33% | CLOSED |
| 3 | 42% | CLOSED |
| 4 | 50% | **OPEN** |
| 5–8 | 50% (sin cambios) | OPEN |

A partir de la quinta, el contador **deja de incrementarse**: las llamadas ya no se hacen, se rechazan localmente. El mensaje cambia a `"Circuito abierto: payment-service no esta disponible"`.

---

### Prueba 4 — Recuperación: Half-Open → Closed

```bash
# Reparar payment con el circuito abierto
curl -X POST http://localhost:8082/simulacion \
  -H "Content-Type: application/json" -d '{"falloActivo": false}'

# Intentar inmediatamente: sigue rechazando aunque el servicio este sano
curl -s -X POST http://localhost:8080/saga/compra \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Test","producto":"Mouse","cantidad":1,"total":150.00,"direccion":"Zona 1"}'

# Esperar el tiempo configurado
sleep 11
curl -s http://localhost:8080/actuator/circuitbreakers \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['circuitBreakers']['payment']['state'])"
# -> HALF_OPEN

# Las tres llamadas de prueba permitidas
for i in {1..3}; do
  curl -s -X POST http://localhost:8080/saga/compra \
    -H "Content-Type: application/json" \
    -d '{"cliente":"Test","producto":"Mouse","cantidad":1,"total":150.00,"direccion":"Zona 1"}' > /dev/null
  curl -s http://localhost:8080/actuator/circuitbreakers \
    | python3 -c "import sys,json; print('  circuito:', json.load(sys.stdin)['circuitBreakers']['payment']['state'])"
done
# -> HALF_OPEN, CLOSED, CLOSED
```

---

### Prueba 5 — Contraste: con y sin Circuit Breaker

```bash
# Shipping (SIN circuit breaker) lento
curl -X POST http://localhost:8084/simulacion \
  -H "Content-Type: application/json" -d '{"retrasoMs": 4000}'

time curl -s -X POST http://localhost:8080/saga/compra \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Test","producto":"Mouse","cantidad":1,"total":150.00,"direccion":"Zona 1"}' > /dev/null
# -> mas de 4 segundos

curl -X POST http://localhost:8084/simulacion \
  -H "Content-Type: application/json" -d '{"retrasoMs": 0}'
```

Con el circuito de Payment abierto, la misma operación falla en **milisegundos**. Eso es "fallar rápido": el beneficio principal del patrón.

---

### Restaurar el estado

```bash
for p in 8082 8083 8084; do
  curl -s -X POST http://localhost:$p/simulacion \
    -H "Content-Type: application/json" \
    -d '{"falloActivo": false, "retrasoMs": 0}' > /dev/null
done
```

---

## Tests automatizados

```bash
cd order-service        && ./mvnw test   # 12 tests
cd payment-service      && ./mvnw test   # 26 tests
cd inventory-service    && ./mvnw test   # 33 tests
cd shipping-service     && ./mvnw test   # 24 tests
cd orchestrator-service && ./mvnw test   # 23 tests
```

Ejecutar una clase concreta:

```bash
./mvnw test -Dtest=PaymentClientCircuitBreakerTest
```

**Los tests más relevantes:**

| Clase | Qué verifica |
|---|---|
| `SagaServiceImplTest` | Compensaciones en orden inverso; solo se compensa lo ejecutado; una compensación fallida deja la saga en `FALLIDA` |
| `PaymentClientCircuitBreakerTest` | Las transiciones Closed → Open → Half-Open → Closed; que las excepciones de negocio sean ignoradas |
| `*ServiceImplTest` | Idempotencia de las compensaciones; que no se borren registros |

---

## Decisiones de diseño

### La compensación no es un rollback

Ninguna compensación borra registros. Cancelar una orden la marca como `CANCELADA`; reembolsar un pago lo marca como `REEMBOLSADO` y conserva la fecha del cobro original.

Esto es deliberado: en una saga el cobro **ocurrió**, y la historia debe conservarse. Una compensación añade un hecho nuevo que anula el efecto del anterior, no reescribe el pasado.

### Las compensaciones son idempotentes

Llamar dos veces a una compensación no produce error ni efectos duplicados. En sistemas distribuidos una compensación puede reintentarse, así que debe tolerar ejecuciones repetidas.

### `ejecutarCompra` no es `@Transactional`

Cada paso ya se confirmó en la base de datos de otro servicio. No existe un rollback que pueda deshacerlo. Anotar el método daría una falsa sensación de atomicidad.

### Las compensaciones no llevan Circuit Breaker

Si el circuito está abierto y aún así hace falta reembolsar, el patrón no debe impedirlo. Compensar tiene prioridad sobre proteger.

### Los fallos de negocio no abren el circuito

`inventory-service` distingue dos tipos de fallo:

| Situación | Código HTTP | ¿Cuenta para el circuito? |
|---|---|---|
| Stock insuficiente | 409 Conflict | **No** |
| Servicio caído | 503 Service Unavailable | Sí |

Un rechazo de negocio significa que el servicio **funciona correctamente**. Si contara para abrir el circuito, un producto agotado dejaría sin servicio a todo el catálogo.

Implementado con `FalloDeNegocioException` y la propiedad:

```properties
resilience4j.circuitbreaker.instances.inventory.ignore-exceptions[0]=com.poc.orchestrator.exceptions.FalloDeNegocioException
```

### Configuración de los Circuit Breakers

```properties
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=5
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=3
```

El `minimum-number-of-calls=5` evita que un único fallo aislado abra el circuito: con una sola llamada fallida la tasa sería 100%.

---

## Problemas frecuentes

**`BindException: Address already in use`**
Otro proceso ocupa el puerto. `kill $(lsof -t -i:8080)`

**`no se ha encontrado o cargado la clase principal ${start-class}`**
Falta la propiedad en el `pom.xml`:
```xml
<start-class>com.poc.order.OrderServiceApplication</start-class>
```

**`/actuator/circuitbreakers` devuelve una lista vacía**
Falta `spring-boot-starter-aspectj` (en Boot 4 ya no se llama `-aop`). Sin AOP las anotaciones `@CircuitBreaker` se ignoran en silencio.

**CORS bloqueado al consultar Actuator desde el frontend**
Actuator tiene configuración de CORS propia, independiente de `WebMvcConfigurer`:
```properties
management.endpoints.web.cors.allowed-origins=http://localhost:5173
management.endpoints.web.cors.allowed-methods=GET,POST,OPTIONS
```

**NetBeans marca errores de Lombok pero Maven compila bien**
Limitación conocida del IDE con JDK 17+. El build funciona; son marcas cosméticas del editor.

**`Flyway: Validate failed`**
Una migración cambió después de aplicarse. En desarrollo:
```bash
psql -U postgres -d orders_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

**La saga siempre falla**
Revisar que las simulaciones estén desactivadas: `curl http://localhost:8082/simulacion`

---

## Referencias

- Richardson, C. [*Pattern: Saga*](https://microservices.io/patterns/data/saga.html) — microservices.io
- Richardson, C. (2018). *Microservices Patterns*. Manning.
- Fowler, M. (2014). [*CircuitBreaker*](https://martinfowler.com/bliki/CircuitBreaker.html) — martinfowler.com
- Nygard, M. (2018). *Release It!* (2.ª ed.). Pragmatic Bookshelf.
- Garcia-Molina, H. & Salem, K. (1987). [*Sagas*](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf). ACM SIGMOD.
- [Resilience4j — CircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)