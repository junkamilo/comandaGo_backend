package com.comandago.api.pedido.service;

import com.comandago.api.shared.security.UsuarioPrincipal;
import org.mockito.Mockito;

final class TestSecurityContext {

    private TestSecurityContext() {
    }

    static void runAs(UsuarioPrincipal user, Runnable action) {
        var security = Mockito.mockStatic(com.comandago.api.shared.security.SecurityUtils.class);
        try {
            security.when(() -> com.comandago.api.shared.security.SecurityUtils.currentUserOrNull())
                    .thenReturn(user);
            action.run();
        } finally {
            security.close();
        }
    }
}
