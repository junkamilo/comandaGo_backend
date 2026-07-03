package com.comandago.api.usuario.dto.request;

import com.comandago.api.usuario.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioUpdateRequest {

    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Email(message = "El email no es válido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    private String email;

    @Pattern(regexp = "^[0-9+\\-() ]{7,30}$", message = "El teléfono no es válido")
    private String telefono;

    private Rol rol;
}
