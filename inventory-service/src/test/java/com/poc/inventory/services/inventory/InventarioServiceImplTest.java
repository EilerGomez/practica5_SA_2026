/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.inventory.services.inventory;

/**
 *
 * @author eiler
 */
import com.poc.inventory.dtoInventario.ProductoResponse;
import com.poc.inventory.dtoInventario.ReservaRequest;
import com.poc.inventory.dtoInventario.ReservaResponse;
import com.poc.inventory.exceptions.InventarioNoDisponibleException;
import com.poc.inventory.exceptions.StockInsuficienteException;
import com.poc.inventory.models.inventory.EntidadProducto;
import com.poc.inventory.models.inventory.EntidadReserva;
import com.poc.inventory.models.inventory.EstadoReserva;
import com.poc.inventory.repositories.inventory.ProductoRepository;
import com.poc.inventory.repositories.inventory.ReservaRepository;
import com.poc.inventory.services.simulacion.SimulacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventarioServiceImpl")
class InventarioServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SimulacionService simulacionService;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    private ReservaRequest request;

    @BeforeEach
    void setUp() {
        request = new ReservaRequest();
        request.setSagaId("saga-001");
        request.setOrdenId(1L);
        request.setProducto("Laptop");
        request.setCantidad(2);

        when(simulacionService.debeFallar()).thenReturn(false);
        when(simulacionService.retrasoMs()).thenReturn(0L);
    }

    // ==================== RESERVAR ====================

    @Test
    @DisplayName("reservar() crea la reserva y aumenta el stock reservado")
    void reservarCreaReservaYActualizaStock() {
        EntidadProducto producto = producto("Laptop", 10, 0);
        when(productoRepository.findByNombreConBloqueo("Laptop"))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(any(EntidadProducto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(EntidadReserva.class)))
                .thenAnswer(inv -> {
                    EntidadReserva r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        ReservaResponse response = inventarioService.reservar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEstado()).isEqualTo("RESERVADO");
        assertThat(response.getCantidad()).isEqualTo(2);
        assertThat(response.getFechaLiberacion()).isNull();

        ArgumentCaptor<EntidadProducto> captor = ArgumentCaptor.forClass(EntidadProducto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getStockReservado()).isEqualTo(2);
        assertThat(captor.getValue().getStockDisponible()).isEqualTo(8);
    }

    @Test
    @DisplayName("reservar() lanza StockInsuficienteException cuando no alcanza el stock")
    void reservarSinStockSuficiente() {
        request.setProducto("Monitor");
        request.setCantidad(5);

        when(productoRepository.findByNombreConBloqueo("Monitor"))
                .thenReturn(Optional.of(producto("Monitor", 3, 0)));

        assertThatThrownBy(() -> inventarioService.reservar(request))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Monitor")
                .hasMessageContaining("5")
                .hasMessageContaining("3");

        // Si no hay stock, no se persiste nada
        verify(reservaRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("reservar() considera el stock ya reservado por otras sagas")
    void reservarConsideraStockYaReservado() {
        // 10 totales, 9 ya reservados, solo queda 1 disponible
        when(productoRepository.findByNombreConBloqueo("Laptop"))
                .thenReturn(Optional.of(producto("Laptop", 10, 9)));

        assertThatThrownBy(() -> inventarioService.reservar(request))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible: 1");
    }

    @Test
    @DisplayName("reservar() permite consumir exactamente el stock disponible")
    void reservarConsumeStockExacto() {
        request.setCantidad(3);
        EntidadProducto producto = producto("Monitor", 3, 0);
        request.setProducto("Monitor");

        when(productoRepository.findByNombreConBloqueo("Monitor"))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(any(EntidadProducto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(EntidadReserva.class)))
                .thenAnswer(inv -> {
                    EntidadReserva r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        ReservaResponse response = inventarioService.reservar(request);

        assertThat(response.getEstado()).isEqualTo("RESERVADO");
        assertThat(producto.getStockDisponible()).isZero();
    }

    @Test
    @DisplayName("reservar() lanza InventarioNoDisponibleException con la simulacion activa")
    void reservarFallaConSimulacionActiva() {
        when(simulacionService.debeFallar()).thenReturn(true);

        assertThatThrownBy(() -> inventarioService.reservar(request))
                .isInstanceOf(InventarioNoDisponibleException.class);

        // El fallo de infraestructura ocurre antes de tocar la base
        verify(productoRepository, never()).findByNombreConBloqueo(any());
    }

    @Test
    @DisplayName("reservar() lanza excepcion si el producto no existe")
    void reservarProductoInexistente() {
        request.setProducto("Impresora");
        when(productoRepository.findByNombreConBloqueo("Impresora"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventarioService.reservar(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Impresora");
    }

    @Test
    @DisplayName("reservar() aplica el retraso configurado")
    void reservarAplicaRetraso() {
        when(simulacionService.retrasoMs()).thenReturn(300L);
        when(productoRepository.findByNombreConBloqueo("Laptop"))
                .thenReturn(Optional.of(producto("Laptop", 10, 0)));
        when(productoRepository.save(any(EntidadProducto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(EntidadReserva.class)))
                .thenAnswer(inv -> {
                    EntidadReserva r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        long inicio = System.currentTimeMillis();
        inventarioService.reservar(request);
        long transcurrido = System.currentTimeMillis() - inicio;

        assertThat(transcurrido).isGreaterThanOrEqualTo(300L);
    }

    // ==================== LIBERAR (COMPENSACION) ====================

    @Test
    @DisplayName("liberar() devuelve las unidades al stock disponible")
    void liberarDevuelveStock() {
        EntidadProducto producto = producto("Laptop", 10, 2);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaActiva()));
        when(productoRepository.findByNombreConBloqueo("Laptop"))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(any(EntidadProducto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(EntidadReserva.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse response = inventarioService.liberar(1L);

        assertThat(response.getEstado()).isEqualTo("LIBERADO");
        assertThat(response.getFechaLiberacion()).isNotNull();
        assertThat(producto.getStockReservado()).isZero();
        assertThat(producto.getStockDisponible()).isEqualTo(10);

        // La compensacion NO borra el registro
        verify(reservaRepository, never()).delete(any());
        verify(reservaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("liberar() es idempotente: no toca el stock si ya estaba liberada")
    void liberarEsIdempotente() {
        EntidadReserva yaLiberada = reservaActiva();
        yaLiberada.setEstado(EstadoReserva.LIBERADO);
        yaLiberada.setFechaLiberacion(LocalDateTime.now());

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(yaLiberada));

        ReservaResponse response = inventarioService.liberar(1L);

        assertThat(response.getEstado()).isEqualTo("LIBERADO");
        verify(productoRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("liberar() nunca deja el stock reservado en negativo")
    void liberarNoDejaStockNegativo() {
        // Escenario anomalo: la reserva pide devolver mas de lo reservado
        EntidadProducto producto = producto("Laptop", 10, 1);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaActiva()));
        when(productoRepository.findByNombreConBloqueo("Laptop"))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(any(EntidadProducto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(EntidadReserva.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        inventarioService.liberar(1L);

        assertThat(producto.getStockReservado()).isZero();
    }

    @Test
    @DisplayName("liberar() lanza excepcion si la reserva no existe")
    void liberarReservaInexistente() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventarioService.liberar(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ==================== CONSULTAS ====================

    @Test
    @DisplayName("listarProductos() calcula el stock disponible")
    void listarProductosCalculaDisponible() {
        when(productoRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(producto("Laptop", 10, 3), producto("Monitor", 3, 3)));

        List<ProductoResponse> lista = inventarioService.listarProductos();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getStockDisponible()).isEqualTo(7);
        assertThat(lista.get(1).getStockDisponible()).isZero();
    }

    @Test
    @DisplayName("listarReservas() devuelve las reservas mapeadas a DTO")
    void listarReservasDevuelveTodas() {
        EntidadReserva a = reservaActiva();
        EntidadReserva b = reservaActiva();
        b.setId(2L);
        b.setSagaId("saga-002");

        when(reservaRepository.findAllByOrderByIdDesc()).thenReturn(List.of(b, a));

        List<ReservaResponse> lista = inventarioService.listarReservas();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getSagaId()).isEqualTo("saga-002");
    }

    // ==================== HELPERS ====================

    private EntidadProducto producto(String nombre, int total, int reservado) {
        EntidadProducto p = new EntidadProducto();
        p.setId(1L);
        p.setNombre(nombre);
        p.setStockTotal(total);
        p.setStockReservado(reservado);
        return p;
    }

    private EntidadReserva reservaActiva() {
        EntidadReserva r = new EntidadReserva();
        r.setId(1L);
        r.setSagaId("saga-001");
        r.setOrdenId(1L);
        r.setProducto("Laptop");
        r.setCantidad(2);
        r.setEstado(EstadoReserva.RESERVADO);
        r.setFechaReserva(LocalDateTime.now());
        return r;
    }
}
