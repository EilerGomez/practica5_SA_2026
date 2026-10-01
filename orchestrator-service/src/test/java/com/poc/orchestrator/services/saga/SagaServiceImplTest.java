/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.poc.orchestrator.services.saga;

/**
 *
 * @author eiler
 */
import com.poc.orchestrator.clients.inventory.InventoryClient;
import com.poc.orchestrator.clients.order.OrderClient;
import com.poc.orchestrator.clients.payment.PaymentClient;
import com.poc.orchestrator.clients.shipping.ShippingClient;
import com.poc.orchestrator.dtoClientes.*;
import com.poc.orchestrator.dtoSaga.CompraRequest;
import com.poc.orchestrator.dtoSaga.SagaResponse;
import com.poc.orchestrator.exceptions.PasoFallidoException;
import com.poc.orchestrator.models.saga.*;
import com.poc.orchestrator.repositories.saga.SagaPasoRepository;
import com.poc.orchestrator.repositories.saga.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SagaServiceImpl")
class SagaServiceImplTest {

    @Mock private SagaRepository sagaRepository;
    @Mock private SagaPasoRepository sagaPasoRepository;
    @Mock private OrderClient orderClient;
    @Mock private PaymentClient paymentClient;
    @Mock private InventoryClient inventoryClient;
    @Mock private ShippingClient shippingClient;

    @InjectMocks
    private SagaServiceImpl sagaService;

    private CompraRequest request;

    @BeforeEach
    void setUp() {
        request = new CompraRequest();
        request.setCliente("Eiler");
        request.setProducto("Laptop");
        request.setCantidad(1);
        request.setTotal(new BigDecimal("8500.00"));
        request.setDireccion("Zona 3, Quetzaltenango");

        when(sagaRepository.save(any(EntidadSaga.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(sagaPasoRepository.save(any(EntidadSagaPaso.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== FLUJO FELIZ ====================

    @Test
    @DisplayName("Ejecuta los cuatro pasos en orden y la saga queda COMPLETADA")
    void flujoFelizEjecutaCuatroPasos() {
        configurarPasosExitosos();

        SagaResponse response = sagaService.ejecutarCompra(request);

        assertThat(response.getEstado()).isEqualTo("COMPLETADA");
        assertThat(response.getOrdenId()).isEqualTo(10L);
        assertThat(response.getPagoId()).isEqualTo(20L);
        assertThat(response.getReservaId()).isEqualTo(30L);
        assertThat(response.getEnvioId()).isEqualTo(40L);
        assertThat(response.getMotivoFallo()).isNull();
        assertThat(response.getFechaFin()).isNotNull();

        // Los pasos ocurren en el orden definido por la saga
        InOrder orden = inOrder(orderClient, paymentClient, inventoryClient, shippingClient);
        orden.verify(orderClient).crear(anyString(), anyString(), anyString(), anyInt(), any());
        orden.verify(paymentClient).cobrar(anyString(), anyLong(), anyString(), any());
        orden.verify(inventoryClient).reservar(anyString(), anyLong(), anyString(), anyInt());
        orden.verify(shippingClient).programar(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("El flujo feliz no ejecuta ninguna compensacion")
    void flujoFelizNoCompensa() {
        configurarPasosExitosos();

        sagaService.ejecutarCompra(request);

        verify(shippingClient, never()).cancelar(any());
        verify(inventoryClient, never()).liberar(any());
        verify(paymentClient, never()).reembolsar(any());
        verify(orderClient, never()).cancelar(any());
    }

    @Test
    @DisplayName("Genera un sagaId unico con el prefijo SAGA-")
    void generaSagaIdUnico() {
        configurarPasosExitosos();

        String id1 = sagaService.ejecutarCompra(request).getSagaId();
        String id2 = sagaService.ejecutarCompra(request).getSagaId();

        assertThat(id1).startsWith("SAGA-");
        assertThat(id1).isNotEqualTo(id2);
    }

    // ==================== COMPENSACIONES ====================

    @Test
    @DisplayName("Si falla INVENTARIO, compensa PAGO y ORDEN en orden inverso")
    void falloEnInventarioCompensaDosPasos() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenReturn(pagoResponse(20L));
        when(inventoryClient.reservar(anyString(), anyLong(), anyString(), anyInt()))
                .thenThrow(new PasoFallidoException("INVENTARIO", "Stock insuficiente", true));

        SagaResponse response = sagaService.ejecutarCompra(request);

        assertThat(response.getEstado()).isEqualTo("COMPENSADA");
        assertThat(response.getMotivoFallo()).contains("Stock insuficiente");

        // ORDEN INVERSO: primero el pago, luego la orden
        InOrder orden = inOrder(paymentClient, orderClient);
        orden.verify(paymentClient).reembolsar(20L);
        orden.verify(orderClient).cancelar(10L);

        // No se compensa lo que nunca se ejecuto
        verify(inventoryClient, never()).liberar(any());
        verify(shippingClient, never()).cancelar(any());
        verify(shippingClient, never()).programar(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Si falla ENVIO, compensa los tres pasos anteriores en orden inverso")
    void falloEnEnvioCompensaTresPasos() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenReturn(pagoResponse(20L));
        when(inventoryClient.reservar(anyString(), anyLong(), anyString(), anyInt()))
                .thenReturn(reservaResponse(30L));
        when(shippingClient.programar(anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new PasoFallidoException("ENVIO", "Servicio no disponible", false));

        SagaResponse response = sagaService.ejecutarCompra(request);

        assertThat(response.getEstado()).isEqualTo("COMPENSADA");

        // INVENTARIO -> PAGO -> ORDEN
        InOrder orden = inOrder(inventoryClient, paymentClient, orderClient);
        orden.verify(inventoryClient).liberar(30L);
        orden.verify(paymentClient).reembolsar(20L);
        orden.verify(orderClient).cancelar(10L);

        verify(shippingClient, never()).cancelar(any());
    }

    @Test
    @DisplayName("Si falla PAGO, solo compensa ORDEN")
    void falloEnPagoCompensaSoloOrden() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenThrow(new PasoFallidoException("PAGO", "Pago rechazado", false));

        SagaResponse response = sagaService.ejecutarCompra(request);

        assertThat(response.getEstado()).isEqualTo("COMPENSADA");

        verify(orderClient).cancelar(10L);
        verify(paymentClient, never()).reembolsar(any());
        verify(inventoryClient, never()).liberar(any());
        verify(shippingClient, never()).cancelar(any());
    }

    @Test
    @DisplayName("Si falla ORDEN, no hay nada que compensar")
    void falloEnOrdenNoCompensa() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new PasoFallidoException("ORDEN", "Servicio caido", false));

        SagaResponse response = sagaService.ejecutarCompra(request);

        assertThat(response.getEstado()).isEqualTo("COMPENSADA");

        verify(orderClient, never()).cancelar(any());
        verify(paymentClient, never()).reembolsar(any());
        verify(inventoryClient, never()).liberar(any());
        verify(shippingClient, never()).cancelar(any());
    }

    // ==================== COMPENSACION FALLIDA ====================

    @Test
    @DisplayName("Si una compensacion falla, la saga queda FALLIDA")
    void compensacionFallidaDejaSagaEnFallida() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenReturn(pagoResponse(20L));
        when(inventoryClient.reservar(anyString(), anyLong(), anyString(), anyInt()))
                .thenThrow(new PasoFallidoException("INVENTARIO", "Stock insuficiente", true));

        // El reembolso tambien falla: peor escenario posible
        doThrow(new RuntimeException("payment-service caido"))
                .when(paymentClient).reembolsar(20L);

        SagaResponse response = sagaService.ejecutarCompra(request);

        assertThat(response.getEstado()).isEqualTo("FALLIDA");
    }

    @Test
    @DisplayName("Una compensacion fallida no impide que las demas se ejecuten")
    void compensacionFallidaNoDetieneLasDemas() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenReturn(pagoResponse(20L));
        when(inventoryClient.reservar(anyString(), anyLong(), anyString(), anyInt()))
                .thenReturn(reservaResponse(30L));
        when(shippingClient.programar(anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new PasoFallidoException("ENVIO", "No disponible", false));

        // Falla la liberacion de inventario
        doThrow(new RuntimeException("inventory-service caido"))
                .when(inventoryClient).liberar(30L);

        sagaService.ejecutarCompra(request);

        // Las compensaciones siguientes se ejecutan igual
        verify(paymentClient).reembolsar(20L);
        verify(orderClient).cancelar(10L);
    }

    // ==================== REGISTRO DE PASOS ====================

    @Test
    @DisplayName("Registra los cuatro pasos como TRANSACCION EXITOSO en el flujo feliz")
    void registraPasosDelFlujoFeliz() {
        configurarPasosExitosos();

        sagaService.ejecutarCompra(request);

        ArgumentCaptor<EntidadSagaPaso> captor =
                ArgumentCaptor.forClass(EntidadSagaPaso.class);
        verify(sagaPasoRepository, times(4)).save(captor.capture());

        List<EntidadSagaPaso> pasos = captor.getAllValues();
        assertThat(pasos).extracting(EntidadSagaPaso::getPaso)
                .containsExactly("ORDEN", "PAGO", "INVENTARIO", "ENVIO");
        assertThat(pasos).allMatch(p -> p.getTipo() == TipoPaso.TRANSACCION);
        assertThat(pasos).allMatch(p -> p.getEstado() == EstadoPaso.EXITOSO);
    }

    @Test
    @DisplayName("Registra el fallo y las compensaciones en la traza")
    void registraFalloYCompensaciones() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenReturn(pagoResponse(20L));
        when(inventoryClient.reservar(anyString(), anyLong(), anyString(), anyInt()))
                .thenThrow(new PasoFallidoException("INVENTARIO", "Stock insuficiente", true));

        sagaService.ejecutarCompra(request);

        ArgumentCaptor<EntidadSagaPaso> captor =
                ArgumentCaptor.forClass(EntidadSagaPaso.class);
        verify(sagaPasoRepository, atLeast(5)).save(captor.capture());

        List<EntidadSagaPaso> pasos = captor.getAllValues();

        // Dos transacciones exitosas, un fallo, dos compensaciones
        assertThat(pasos).filteredOn(p -> p.getTipo() == TipoPaso.TRANSACCION
                        && p.getEstado() == EstadoPaso.EXITOSO).hasSize(2);
        assertThat(pasos).filteredOn(p -> p.getEstado() == EstadoPaso.FALLIDO).hasSize(1);
        assertThat(pasos).filteredOn(p -> p.getTipo() == TipoPaso.COMPENSACION).hasSize(2);
    }

    @Test
    @DisplayName("Marca el paso como CIRCUITO_ABIERTO cuando el circuito rechaza la llamada")
    void registraCircuitoAbierto() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenThrow(new PasoFallidoException("PAGO",
                        "Circuito abierto: payment-service no esta disponible", false));

        sagaService.ejecutarCompra(request);

        ArgumentCaptor<EntidadSagaPaso> captor =
                ArgumentCaptor.forClass(EntidadSagaPaso.class);
        verify(sagaPasoRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues())
                .anyMatch(p -> p.getEstado() == EstadoPaso.CIRCUITO_ABIERTO);
    }

    // ==================== HELPERS ====================

    private void configurarPasosExitosos() {
        when(orderClient.crear(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(ordenResponse(10L));
        when(paymentClient.cobrar(anyString(), anyLong(), anyString(), any()))
                .thenReturn(pagoResponse(20L));
        when(inventoryClient.reservar(anyString(), anyLong(), anyString(), anyInt()))
                .thenReturn(reservaResponse(30L));
        when(shippingClient.programar(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(envioResponse(40L));
    }

    private OrdenClienteResponse ordenResponse(Long id) {
        OrdenClienteResponse r = new OrdenClienteResponse();
        r.setId(id);
        r.setEstado("CREADA");
        return r;
    }

    private PagoClienteResponse pagoResponse(Long id) {
        PagoClienteResponse r = new PagoClienteResponse();
        r.setId(id);
        r.setEstado("COBRADO");
        r.setReferencia("REF-TEST123");
        return r;
    }

    private ReservaClienteResponse reservaResponse(Long id) {
        ReservaClienteResponse r = new ReservaClienteResponse();
        r.setId(id);
        r.setEstado("RESERVADO");
        return r;
    }

    private EnvioClienteResponse envioResponse(Long id) {
        EnvioClienteResponse r = new EnvioClienteResponse();
        r.setId(id);
        r.setEstado("PROGRAMADO");
        r.setGuia("GUIA-TEST123");
        return r;
    }
}
