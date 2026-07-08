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
import com.comandago.api.shared.security.SecurityUtils;
import com.comandago.api.shared.security.UsuarioPrincipal;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CierreCajaServiceImpl implements CierreCajaService {

    private static final ZoneId ZONA_CO = ZoneId.of("America/Bogota");

    private final CierreCajaRepository cierreRepository;
    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CierreCajaMapper cierreCajaMapper;

    @Override
    @Transactional(readOnly = true)
    public PreviewCierreResponse preview() {
        OffsetDateTime apertura = calcularFechaApertura();
        OffsetDateTime ahora = OffsetDateTime.now(ZONA_CO);
        return calcularTotales(apertura, ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CierreCajaResponse> listar() {
        return cierreRepository.findAllByOrderByFechaCierreDesc().stream()
                .map(cierreCajaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CierreCajaResponse obtener(Long id) {
        return cierreRepository.findById(id)
                .map(cierreCajaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cierre de caja no encontrado con id: " + id));
    }

    @Override
    @Transactional
    public CierreCajaResponse cerrar(CerrarCajaRequest request) {
        OffsetDateTime apertura = calcularFechaApertura();
        OffsetDateTime ahora = OffsetDateTime.now(ZONA_CO);

        if (cierreRepository.existsCierreDespuesDe(apertura)) {
            throw new BusinessException("Ya existe un cierre de caja para este turno");
        }

        PreviewCierreResponse totales = calcularTotales(apertura, ahora);

        BigDecimal diferencia = null;
        if (request.efectivoContado() != null) {
            diferencia = request.efectivoContado().subtract(totales.totalEfectivo());
        }

        CierreCaja cierre = new CierreCaja();
        cierre.setUsuario(resolverCajero());
        cierre.setFechaApertura(apertura);
        cierre.setFechaCierre(ahora);
        cierre.setTotalEfectivo(totales.totalEfectivo());
        cierre.setTotalTarjeta(totales.totalTarjeta());
        cierre.setTotalNequi(totales.totalNequi());
        cierre.setTotalDaviplata(totales.totalDaviplata());
        cierre.setTotalTransferencia(totales.totalTransferencia());
        cierre.setTotalOtros(totales.totalOtros());
        cierre.setTotalPropinas(totales.totalPropinas());
        cierre.setTotalGeneral(totales.totalGeneral());
        cierre.setEfectivoContado(request.efectivoContado());
        cierre.setDiferencia(diferencia);
        cierre.setPedidosAtendidos(totales.pedidosAtendidos());
        cierre.setPedidosCancelados(totales.pedidosCancelados());
        cierre.setNotas(request.notas());

        return cierreCajaMapper.toResponse(cierreRepository.save(cierre));
    }

    private OffsetDateTime calcularFechaApertura() {
        return cierreRepository.findTopByOrderByFechaCierreDesc()
                .map(CierreCaja::getFechaCierre)
                .orElse(LocalDate.now(ZONA_CO).atStartOfDay(ZONA_CO).toOffsetDateTime());
    }

    private PreviewCierreResponse calcularTotales(OffsetDateTime desde, OffsetDateTime hasta) {
        BigDecimal totalEfectivo = pagoRepository.totalPorMetodoEnRango(MetodoPago.EFECTIVO, desde, hasta);
        BigDecimal totalTarjeta = pagoRepository.totalPorMetodoEnRango(MetodoPago.TARJETA, desde, hasta);
        BigDecimal totalNequi = pagoRepository.totalPorMetodoEnRango(MetodoPago.NEQUI, desde, hasta);
        BigDecimal totalDaviplata = pagoRepository.totalPorMetodoEnRango(MetodoPago.DAVIPLATA, desde, hasta);
        BigDecimal totalTransferencia = pagoRepository.totalPorMetodoEnRango(
                MetodoPago.TRANSFERENCIA, desde, hasta);

        BigDecimal totalPse = pagoRepository.totalPorMetodoEnRango(MetodoPago.PSE, desde, hasta);
        BigDecimal totalOtro = pagoRepository.totalPorMetodoEnRango(MetodoPago.OTRO, desde, hasta);
        BigDecimal totalOtros = totalPse.add(totalOtro);

        BigDecimal totalPropinas = pagoRepository.totalPropinasEnRango(desde, hasta);

        BigDecimal totalGeneral = totalEfectivo
                .add(totalTarjeta)
                .add(totalNequi)
                .add(totalDaviplata)
                .add(totalTransferencia)
                .add(totalOtros);

        int atendidos = (int) pedidoRepository.contarPorEstadoEnRango(
                List.of(EstadoPedido.ENTREGADO), desde, hasta);
        int cancelados = (int) pedidoRepository.contarPorEstadoEnRango(
                List.of(EstadoPedido.CANCELADO), desde, hasta);

        return new PreviewCierreResponse(
                desde,
                hasta,
                totalEfectivo,
                totalTarjeta,
                totalNequi,
                totalDaviplata,
                totalTransferencia,
                totalOtros,
                totalPropinas,
                totalGeneral,
                atendidos,
                cancelados
        );
    }

    private Usuario resolverCajero() {
        UsuarioPrincipal principal = SecurityUtils.currentUserOrNull();
        if (principal == null) {
            throw new BusinessException("No se encontró el usuario del cajero");
        }
        return usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("No se encontró el usuario del cajero"));
    }
}
