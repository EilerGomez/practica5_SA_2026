import { useState, useEffect, useCallback } from "react";
import {
  ejecutarCompra, obtenerDetalleSaga, listarSagas,
  estadoCircuitos, listarProductos,
  configurarSimulacion, estadoSimulacion, SERVICIOS,
} from "./api";
import "./App.css";
import TablasServicios from "./TablasServicios";

export default function App() {
  const [form, setForm] = useState({
    cliente: "Eiler",
    producto: "Laptop",
    cantidad: 1,
    total: 8500.0,
    direccion: "Zona 3, Quetzaltenango",
  });

  const [cargando, setCargando] = useState(false);
  const [sagaActual, setSagaActual] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [circuitos, setCircuitos] = useState({});
  const [productos, setProductos] = useState([]);
  const [simulaciones, setSimulaciones] = useState({});

  const refrescar = useCallback(async () => {
    try {
      const [cb, prods, sagas] = await Promise.all([
        estadoCircuitos(), listarProductos(), listarSagas(),
      ]);
      setCircuitos(cb);
      setProductos(prods);
      setHistorial(sagas.slice(0, 10));

      const sims = {};
      for (const [key, s] of Object.entries(SERVICIOS)) {
        try { sims[key] = await estadoSimulacion(s.puerto); } catch { sims[key] = null; }
      }
      setSimulaciones(sims);
    } catch (e) {
      console.error("Error al refrescar:", e);
    }
  }, []);

  useEffect(() => {
    refrescar();
    const id = setInterval(refrescar, 2000);
    return () => clearInterval(id);
  }, [refrescar]);

  async function comprar() {
    setCargando(true);
    setSagaActual(null);
    const inicio = Date.now();
    try {
      const resultado = await ejecutarCompra({
        ...form,
        cantidad: Number(form.cantidad),
        total: Number(form.total),
      });
      const detalle = await obtenerDetalleSaga(resultado.sagaId);
      setSagaActual({ ...detalle, duracion: Date.now() - inicio });
    } catch (e) {
      alert("Error: " + e.message);
    } finally {
      setCargando(false);
      refrescar();
    }
  }

  async function toggleFallo(key) {
    const servicio = SERVICIOS[key];
    const actual = simulaciones[key]?.falloActivo ?? false;
    await configurarSimulacion(servicio.puerto, { falloActivo: !actual });
    refrescar();
  }

  async function toggleLento(key) {
    const servicio = SERVICIOS[key];
    const actual = simulaciones[key]?.retrasoMs ?? 0;
    await configurarSimulacion(servicio.puerto, { retrasoMs: actual > 0 ? 0 : 4000 });
    refrescar();
  }

  return (
    <div className="app">
      <header>
        <h1>Saga Pattern + Circuit Breaker</h1>
      </header>

      <div className="grid">
        {/* ---------- Columna izquierda ---------- */}
        <div className="col">
          <section className="card">
            <h2>Nueva compra</h2>
            <label>Cliente
              <input value={form.cliente}
                     onChange={e => setForm({ ...form, cliente: e.target.value })} />
            </label>
            <label>Producto
              <select value={form.producto}
                      onChange={e => setForm({ ...form, producto: e.target.value })}>
                {productos.map(p => (
                  <option key={p.id} value={p.nombre}>
                    {p.nombre} ({p.stockDisponible} disponibles)
                  </option>
                ))}
              </select>
            </label>
            <div className="fila">
              <label>Cantidad
                <input type="number" min="1" value={form.cantidad}
                       onChange={e => setForm({ ...form, cantidad: e.target.value })} />
              </label>
              <label>Total (Q)
                <input type="number" step="0.01" value={form.total}
                       onChange={e => setForm({ ...form, total: e.target.value })} />
              </label>
            </div>
            <label>Dirección
              <input value={form.direccion}
                     onChange={e => setForm({ ...form, direccion: e.target.value })} />
            </label>
            <button className="principal" onClick={comprar} disabled={cargando}>
              {cargando ? "Ejecutando saga..." : "Ejecutar compra"}
            </button>
          </section>

          <section className="card">
            <h2>Simulación de fallos</h2>
            <p className="ayuda">Provoca fallos en los servicios para ver los patrones en acción.</p>
            {Object.entries(SERVICIOS).map(([key, s]) => (
              <div key={key} className="sim-fila">
                <span className="sim-nombre">{s.nombre}</span>
                <button className={simulaciones[key]?.falloActivo ? "sim activo" : "sim"}
                        onClick={() => toggleFallo(key)}>
                  {simulaciones[key]?.falloActivo ? "Caído" : "Sano"}
                </button>
                <button className={simulaciones[key]?.retrasoMs > 0 ? "sim lento" : "sim"}
                        onClick={() => toggleLento(key)}>
                  {simulaciones[key]?.retrasoMs > 0 ? "Lento 4s" : "Rápido"}
                </button>
              </div>
            ))}
          </section>

          <section className="card">
            <h2>Stock disponible</h2>
            <table className="tabla">
              <thead><tr><th>Producto</th><th>Total</th><th>Reservado</th><th>Libre</th></tr></thead>
              <tbody>
                {productos.map(p => (
                  <tr key={p.id}>
                    <td>{p.nombre}</td>
                    <td>{p.stockTotal}</td>
                    <td>{p.stockReservado}</td>
                    <td className={p.stockDisponible === 0 ? "agotado" : ""}>
                      {p.stockDisponible}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </div>

        {/* ---------- Columna derecha ---------- */}
        <div className="col">
          <section className="card">
            <h2>Circuit Breakers</h2>
            <p className="ayuda">Se actualiza cada 2 segundos.</p>
            <div className="circuitos">
              {Object.entries(circuitos).map(([nombre, cb]) => (
                <div key={nombre} className={`circuito ${cb.state.toLowerCase()}`}>
                  <div className="circuito-cabecera">
                    <strong>{nombre}</strong>
                    <span className="estado">{cb.state}</span>
                  </div>
                  <div className="circuito-datos">
                    <span>Fallos: {cb.failedCalls}/{cb.bufferedCalls}</span>
                    <span>Tasa: {cb.failureRate}</span>
                    <span>Rechazadas: {cb.notPermittedCalls}</span>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {sagaActual && (
            <section className="card">
              <h2>Traza de la saga</h2>
              <div className={`resumen ${sagaActual.saga.estado.toLowerCase()}`}>
                <div className="resumen-linea">
                  <strong>{sagaActual.saga.sagaId}</strong>
                  <span className="badge">{sagaActual.saga.estado}</span>
                </div>
                <div className="resumen-meta">{sagaActual.duracion} ms</div>
                {sagaActual.saga.motivoFallo && (
                  <div className="motivo">{sagaActual.saga.motivoFallo}</div>
                )}
              </div>

              <ol className="pasos">
                {sagaActual.pasos.map(p => (
                  <li key={p.id} className={`paso ${p.tipo.toLowerCase()} ${p.estado.toLowerCase()}`}>
                    <div className="paso-cabecera">
                      <span className="paso-nombre">{p.paso}</span>
                      <span className="paso-tipo">{p.tipo}</span>
                      <span className="paso-estado">{p.estado}</span>
                    </div>
                    <div className="paso-detalle">{p.detalle}</div>
                  </li>
                ))}
              </ol>
            </section>
          )}

          <section className="card">
            <h2>Últimas sagas</h2>
            <table className="tabla">
              <thead><tr><th>ID</th><th>Producto</th><th>Estado</th></tr></thead>
              <tbody>
                {historial.map(s => (
                  <tr key={s.sagaId} className="clickable"
                      onClick={async () => {
                        const d = await obtenerDetalleSaga(s.sagaId);
                        setSagaActual({ ...d, duracion: null });
                      }}>
                    <td className="mono">{s.sagaId}</td>
                    <td>{s.producto} ×{s.cantidad}</td>
                    <td><span className={`badge ${s.estado.toLowerCase()}`}>{s.estado}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </div>
      </div>
      <TablasServicios />
    </div>
  );
}