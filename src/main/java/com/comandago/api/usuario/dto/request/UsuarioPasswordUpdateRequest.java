package com.comandago.api.usuario.dto.request;

import com.comandago.api.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioPasswordUpdateRequest {

    @NotBlank(message = "La contraseña es obligatoria")
    @StrongPassword
    private String password;
}
