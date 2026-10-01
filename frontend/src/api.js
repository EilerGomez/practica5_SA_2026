const ORQUESTADOR = "http://localhost:8080";

export const SERVICIOS = {
  payment:   { nombre: "Payment",   puerto: 8082, circuito: "payment"   },
  inventory: { nombre: "Inventory", puerto: 8083, circuito: "inventory" },
  shipping:  { nombre: "Shipping",  puerto: 8084, circuito: null        },
};

export async function ejecutarCompra(datos) {
  const res = await fetch(`${ORQUESTADOR}/saga/compra`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos),
  });
  if (!res.ok) throw new Error("Error al ejecutar la compra");
  return res.json();
}

export async function obtenerDetalleSaga(sagaId) {
  const res = await fetch(`${ORQUESTADOR}/saga/${sagaId}`);
  if (!res.ok) throw new Error("Saga no encontrada");
  return res.json();
}

export async function listarSagas() {
  const res = await fetch(`${ORQUESTADOR}/saga`);
  return res.json();
}

export async function estadoCircuitos() {
  const res = await fetch(`${ORQUESTADOR}/actuator/circuitbreakers`);
  const data = await res.json();
  return data.circuitBreakers;
}

export async function listarProductos() {
  const res = await fetch(`http://localhost:8083/inventory/products`);
  return res.json();
}

export async function configurarSimulacion(puerto, config) {
  const res = await fetch(`http://localhost:${puerto}/simulacion`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(config),
  });
  return res.json();
}

export async function estadoSimulacion(puerto) {
  const res = await fetch(`http://localhost:${puerto}/simulacion`);
  return res.json();
}

// ===== Consultas directas a cada microservicio =====

export async function listarOrdenes() {
  const res = await fetch("http://localhost:8081/orders");
  return res.json();
}

export async function listarPagos() {
  const res = await fetch("http://localhost:8082/payments");
  return res.json();
}

export async function listarReservas() {
  const res = await fetch("http://localhost:8083/inventory/reservations");
  return res.json();
}

export async function listarEnvios() {
  const res = await fetch("http://localhost:8084/shipping");
  return res.json();
}