package com.comandago.api.caja.service;

import com.comandago.api.caja.dto.request.CerrarCajaRequest;
import com.comandago.api.caja.dto.response.CierreCajaResponse;
import com.comandago.api.caja.dto.response.PreviewCierreResponse;

import java.util.List;

public interface CierreCajaService {

    PreviewCierreResponse preview();

    CierreCajaResponse cerrar(CerrarCajaRequest request);

    List<CierreCajaResponse> listar();

    CierreCajaResponse obtener(Long id);
}
