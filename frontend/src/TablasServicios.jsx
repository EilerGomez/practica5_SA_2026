import { useState, useEffect, useCallback } from "react";
import {
  listarOrdenes, listarPagos, listarReservas, listarEnvios,
} from "./api";

const PESTANIAS = [
  { id: "ordenes",  etiqueta: "Órdenes",  puerto: 8081 },
  { id: "pagos",    etiqueta: "Pagos",    puerto: 8082 },
  { id: "reservas", etiqueta: "Reservas", puerto: 8083 },
  { id: "envios",   etiqueta: "Envíos",   puerto: 8084 },
];

export default function TablasServicios() {
  const [activa, setActiva] = useState("ordenes");
  const [datos, setDatos] = useState({
    ordenes: [], pagos: [], reservas: [], envios: [],
  });

  const refrescar = useCallback(async () => {
    const resultado = { ...datos };
    const peticiones = [
      ["ordenes", listarOrdenes],
      ["pagos", listarPagos],
      ["reservas", listarReservas],
      ["envios", listarEnvios],
    ];

    for (const [clave, fn] of peticiones) {
      try { resultado[clave] = await fn(); } catch { resultado[clave] = []; }
    }
    setDatos(resultado);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    refrescar();
    const id = setInterval(refrescar, 3000);
    return () => clearInterval(id);
  }, [refrescar]);

  return (
    <section className="card">
      <h2>Datos por servicio</h2>

      <div className="pestanias">
        {PESTANIAS.map(p => (
          <button key={p.id}
                  className={`pestania ${activa === p.id ? "activa" : ""}`}
                  onClick={() => setActiva(p.id)}>
            {p.etiqueta}
            <span className="contador">{datos[p.id]?.length ?? 0}</span>
          </button>
        ))}
      </div>

      <div className="pestania-contenido">
        {activa === "ordenes"  && <TablaOrdenes  filas={datos.ordenes} />}
        {activa === "pagos"    && <TablaPagos    filas={datos.pagos} />}
        {activa === "reservas" && <TablaReservas filas={datos.reservas} />}
        {activa === "envios"   && <TablaEnvios   filas={datos.envios} />}
      </div>
    </section>
  );
}

/* ---------- Tablas ---------- */

function TablaOrdenes({ filas }) {
  if (!filas.length) return <Vacio mensaje="Sin órdenes registradas" />;
  return (
    <table className="tabla">
      <thead>
        <tr><th>ID</th><th>Saga</th><th>Cliente</th><th>Producto</th><th>Total</th><th>Estado</th></tr>
      </thead>
      <tbody>
        {filas.map(o => (
          <tr key={o.id}>
            <td>{o.id}</td>
            <td className="mono">{o.sagaId}</td>
            <td>{o.cliente}</td>
            <td>{o.producto} ×{o.cantidad}</td>
            <td className="num">Q{Number(o.total).toFixed(2)}</td>
            <td><Estado valor={o.estado} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TablaPagos({ filas }) {
  if (!filas.length) return <Vacio mensaje="Sin pagos registrados" />;
  return (
    <table className="tabla">
      <thead>
        <tr><th>ID</th><th>Saga</th><th>Referencia</th><th>Monto</th><th>Estado</th><th>Reembolso</th></tr>
      </thead>
      <tbody>
        {filas.map(p => (
          <tr key={p.id}>
            <td>{p.id}</td>
            <td className="mono">{p.sagaId}</td>
            <td className="mono">{p.referencia}</td>
            <td className="num">Q{Number(p.monto).toFixed(2)}</td>
            <td><Estado valor={p.estado} /></td>
            <td className="fecha">{formatearHora(p.fechaReembolso)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TablaReservas({ filas }) {
  if (!filas.length) return <Vacio mensaje="Sin reservas registradas" />;
  return (
    <table className="tabla">
      <thead>
        <tr><th>ID</th><th>Saga</th><th>Producto</th><th>Cant.</th><th>Estado</th><th>Liberada</th></tr>
      </thead>
      <tbody>
        {filas.map(r => (
          <tr key={r.id}>
            <td>{r.id}</td>
            <td className="mono">{r.sagaId}</td>
            <td>{r.producto}</td>
            <td className="num">{r.cantidad}</td>
            <td><Estado valor={r.estado} /></td>
            <td className="fecha">{formatearHora(r.fechaLiberacion)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TablaEnvios({ filas }) {
  if (!filas.length) return <Vacio mensaje="Sin envíos registrados" />;
  return (
    <table className="tabla">
      <thead>
        <tr><th>ID</th><th>Saga</th><th>Guía</th><th>Dirección</th><th>Estado</th><th>Cancelado</th></tr>
      </thead>
      <tbody>
        {filas.map(e => (
          <tr key={e.id}>
            <td>{e.id}</td>
            <td className="mono">{e.sagaId}</td>
            <td className="mono">{e.guia}</td>
            <td className="truncar">{e.direccion}</td>
            <td><Estado valor={e.estado} /></td>
            <td className="fecha">{formatearHora(e.fechaCancelacion)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/* ---------- Auxiliares ---------- */

function Estado({ valor }) {
  const compensado = ["CANCELADA", "CANCELADO", "REEMBOLSADO", "LIBERADO"];
  const clase = compensado.includes(valor) ? "compensado" : "activo";
  return <span className={`pill ${clase}`}>{valor}</span>;
}

function Vacio({ mensaje }) {
  return <p className="vacio">{mensaje}</p>;
}

function formatearHora(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleTimeString("es-GT", {
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
}