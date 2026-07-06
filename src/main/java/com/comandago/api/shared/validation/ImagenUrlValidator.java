package com.comandago.api.shared.validation;

import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.config.SupabaseStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImagenUrlValidator {

    private static final int MAX_URL_LENGTH = 255;

    private final SupabaseStorageProperties properties;

    public void validar(StorageBucket bucket, String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            throw new BusinessException("La URL de imagen es obligatoria");
        }
        validarContenido(bucket, imagenUrl.trim());
    }

    public void validarOpcional(StorageBucket bucket, String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return;
        }
        validarContenido(bucket, imagenUrl.trim());
    }

    public boolean perteneceAlBucket(StorageBucket bucket, String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return false;
        }
        String base = normalizarBaseUrl(properties.getPublicBaseUrl(bucket));
        if (base.isBlank()) {
            return false;
        }
        return normalizarBaseUrl(imagenUrl).startsWith(base + "/");
    }

    private void validarContenido(StorageBucket bucket, String imagenUrl) {
        if (imagenUrl.length() > MAX_URL_LENGTH) {
            throw new BusinessException("La URL de imagen no puede superar 255 caracteres");
        }
        String base = normalizarBaseUrl(properties.getPublicBaseUrl(bucket));
        if (base.isBlank()) {
            throw new BusinessException("Supabase Storage no está configurado en el servidor");
        }
        if (!normalizarBaseUrl(imagenUrl).startsWith(base + "/")) {
            throw new BusinessException("La URL de imagen no pertenece al almacenamiento autorizado");
        }
        if (!imagenUrl.toLowerCase().endsWith(".webp")) {
            throw new BusinessException("La imagen debe estar en formato WebP");
        }
    }

    private String normalizarBaseUrl(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
