package com.comandago.api.pago.dto.request;

import com.comandago.api.pago.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RegistrarPagoRequest {

    @NotNull(message = "El pedido es obligatorio")
    private Long pedidoId;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodo;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El monto tiene formato inválido")
    private BigDecimal monto;

    @DecimalMin(value = "0.0", message = "La propina no puede ser negativa")
    @Digits(integer = 10, fraction = 2, message = "La propina tiene formato inválido")
    private BigDecimal propina;

    @DecimalMin(value = "0.0", message = "El monto recibido no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El monto recibido tiene formato inválido")
    private BigDecimal montoRecibido;

    @Size(max = 150, message = "La referencia no puede superar 150 caracteres")
    private String referencia;

    @Size(max = 100, message = "El proveedor no puede superar 100 caracteres")
    private String proveedorId;

    private String notas;
}
