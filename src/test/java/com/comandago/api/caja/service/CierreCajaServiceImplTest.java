package com.comandago.api.caja.service;

import com.comandago.api.caja.dto.mapper.CierreCajaMapper;
import com.comandago.api.caja.dto.request.CerrarCajaRequest;
import com.comandago.api.caja.dto.response.CierreCajaResponse;
import com.comandago.api.caja.dto.response.PreviewCierreResponse;
import com.comandago.api.caja.entity.CierreCaja;
import com.comandago.api.caja.repository.CierreCajaRepository;
import com.comandago.api.pago.enums.MetodoPago;
import com.comandago.api.pago.repository.PagoRepository;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.security.UsuarioPrincipal;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CierreCajaServiceImplTest {

    private static final ZoneId ZONA_CO = ZoneId.of("America/Bogota");

    @Mock
    private CierreCajaRepository cierreRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CierreCajaMapper cierreCajaMapper;

    @InjectMocks
    private CierreCajaServiceImpl service;

    @Test
    void preview_sinCierresPrevios_usaInicioDelDia() {
        when(cierreRepository.findTopByOrderByFechaCierreDesc()).thenReturn(Optional.empty());
        stubTotalesCero();

        PreviewCierreResponse result = service.preview();

        OffsetDateTime inicioDia = LocalDate.now(ZONA_CO).atStartOfDay(ZONA_CO).toOffsetDateTime();
        assertThat(result.fechaApertura()).isEqualTo(inicioDia);
        assertThat(result.totalGeneral()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void preview_conCierrePrevio_usaFechaCierreAnterior() {
        OffsetDateTime cierreAnterior = OffsetDateTime.parse("2026-07-08T14:00:00-05:00");
        CierreCaja ultimo = new CierreCaja();
        ultimo.setFechaCierre(cierreAnterior);
        when(cierreRepository.findTopByOrderByFechaCierreDesc()).thenReturn(Optional.of(ultimo));
        stubTotalesCero();

        PreviewCierreResponse result = service.preview();

        assertThat(result.fechaApertura()).isEqualTo(cierreAnterior);
    }

    @Test
    void preview_sumaMetodosYPropinasSeparadas() {
        when(cierreRepository.findTopByOrderByFechaCierreDesc()).thenReturn(Optional.empty());
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.EFECTIVO), any(), any()))
                .thenReturn(new BigDecimal("100000"));
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.TARJETA), any(), any()))
                .thenReturn(new BigDecimal("50000"));
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.NEQUI), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.DAVIPLATA), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.TRANSFERENCIA), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.PSE), any(), any()))
                .thenReturn(new BigDecimal("10000"));
        when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.OTRO), any(), any()))
                .thenReturn(new BigDecimal("5000"));
        when(pagoRepository.totalPropinasEnRango(any(), any())).thenReturn(new BigDecimal("15000"));
        when(pedidoRepository.contarPorEstadoEnRango(eq(List.of(EstadoPedido.ENTREGADO)), any(), any()))
                .thenReturn(3L);
        when(pedidoRepository.contarPorEstadoEnRango(eq(List.of(EstadoPedido.CANCELADO)), any(), any()))
                .thenReturn(1L);

        PreviewCierreResponse result = service.preview();

        assertThat(result.totalEfectivo()).isEqualByComparingTo("100000");
        assertThat(result.totalOtros()).isEqualByComparingTo("15000");
        assertThat(result.totalPropinas()).isEqualByComparingTo("15000");
        assertThat(result.totalGeneral()).isEqualByComparingTo("165000");
        assertThat(result.pedidosAtendidos()).isEqualTo(3);
        assertThat(result.pedidosCancelados()).isEqualTo(1);
    }

    @Test
    void cerrar_persisteConDiferenciaCorrecta() {
        try (var security = org.mockito.Mockito.mockStatic(
                com.comandago.api.shared.security.SecurityUtils.class)) {
            security.when(com.comandago.api.shared.security.SecurityUtils::currentUserOrNull)
                    .thenReturn(new UsuarioPrincipal(1L, "cajero@test.com", "hash", Rol.CAJERO, true));

            Usuario cajero = new Usuario();
            cajero.setId(1L);
            cajero.setNombre("Ana Cajera");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cajero));
            when(cierreRepository.findTopByOrderByFechaCierreDesc()).thenReturn(Optional.empty());
            when(cierreRepository.existsCierreDespuesDe(any())).thenReturn(false);
            stubTotalesCero();
            when(pagoRepository.totalPorMetodoEnRango(eq(MetodoPago.EFECTIVO), any(), any()))
                    .thenReturn(new BigDecimal("520000"));

            CierreCaja guardado = new CierreCaja();
            guardado.setId(10L);
            when(cierreRepository.save(any(CierreCaja.class))).thenAnswer(inv -> {
                CierreCaja c = inv.getArgument(0);
                c.setId(10L);
                return c;
            });
            when(cierreCajaMapper.toResponse(any())).thenReturn(
                    new CierreCajaResponse(
                            10L, "Ana Cajera", null, null,
                            new BigDecimal("520000"), BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("515000"),
                            new BigDecimal("-5000"), 0, 0, null));

            CierreCajaResponse result = service.cerrar(new CerrarCajaRequest(new BigDecimal("515000"), null));

            ArgumentCaptor<CierreCaja> captor = ArgumentCaptor.forClass(CierreCaja.class);
            verify(cierreRepository).save(captor.capture());
            assertThat(captor.getValue().getDiferencia()).isEqualByComparingTo("-5000");
            assertThat(result.diferencia()).isEqualByComparingTo("-5000");
        }
    }

    @Test
    void cerrar_duplicado_lanzaBusinessException() {
        when(cierreRepository.findTopByOrderByFechaCierreDesc()).thenReturn(Optional.empty());
        when(cierreRepository.existsCierreDespuesDe(any())).thenReturn(true);

        assertThatThrownBy(() -> service.cerrar(new CerrarCajaRequest(BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe un cierre");
    }

    @Test
    void cerrar_sinEfectivoContado_diferenciaNull() {
        try (var security = org.mockito.Mockito.mockStatic(
                com.comandago.api.shared.security.SecurityUtils.class)) {
            security.when(com.comandago.api.shared.security.SecurityUtils::currentUserOrNull)
                    .thenReturn(new UsuarioPrincipal(1L, "cajero@test.com", "hash", Rol.CAJERO, true));

            Usuario cajero = new Usuario();
            cajero.setId(1L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cajero));
            when(cierreRepository.findTopByOrderByFechaCierreDesc()).thenReturn(Optional.empty());
            when(cierreRepository.existsCierreDespuesDe(any())).thenReturn(false);
            stubTotalesCero();
            when(cierreRepository.save(any(CierreCaja.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cierreCajaMapper.toResponse(any())).thenReturn(
                    new CierreCajaResponse(
                            1L, null, null, null,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, null, null, 0, 0, null));

            service.cerrar(new CerrarCajaRequest(null, "sin conteo"));

            ArgumentCaptor<CierreCaja> captor = ArgumentCaptor.forClass(CierreCaja.class);
            verify(cierreRepository).save(captor.capture());
            assertThat(captor.getValue().getDiferencia()).isNull();
            assertThat(captor.getValue().getNotas()).isEqualTo("sin conteo");
        }
    }

    @Test
    void obtener_inexistente_lanzaResourceNotFoundException() {
        when(cierreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubTotalesCero() {
        when(pagoRepository.totalPorMetodoEnRango(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.totalPropinasEnRango(any(), any())).thenReturn(BigDecimal.ZERO);
        when(pedidoRepository.contarPorEstadoEnRango(any(), any(), any())).thenReturn(0L);
    }
}
