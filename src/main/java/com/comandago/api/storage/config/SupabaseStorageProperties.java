package com.comandago.api.storage.config;

import com.comandago.api.storage.StorageBucket;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.supabase")
public class SupabaseStorageProperties {

    private String url = "";
    private String serviceRoleKey = "";
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Storage {
        private String bucketCategorias = "categorias";
        private String bucketProductos = "productos";
        private String publicBaseUrlCategorias = "";
        private String publicBaseUrlProductos = "";
    }

    public boolean isConfigured() {
        return missingConfigurationReason() == null;
    }

    /** null si está listo; texto con el primer valor faltante. */
    public String missingConfigurationReason() {
        if (url == null || url.isBlank()) {
            return "APP_SUPABASE_URL";
        }
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            return "APP_SUPABASE_SERVICE_ROLE_KEY";
        }
        if (storage.publicBaseUrlCategorias == null || storage.publicBaseUrlCategorias.isBlank()) {
            return "APP_SUPABASE_PUBLIC_BASE_URL_CATEGORIAS";
        }
        if (storage.publicBaseUrlProductos == null || storage.publicBaseUrlProductos.isBlank()) {
            return "APP_SUPABASE_PUBLIC_BASE_URL_PRODUCTOS";
        }
        return null;
    }

    public String getBucketName(StorageBucket bucket) {
        return switch (bucket) {
            case CATEGORIAS -> storage.bucketCategorias;
            case PRODUCTOS -> storage.bucketProductos;
        };
    }

    public String getPublicBaseUrl(StorageBucket bucket) {
        return switch (bucket) {
            case CATEGORIAS -> storage.publicBaseUrlCategorias;
            case PRODUCTOS -> storage.publicBaseUrlProductos;
        };
    }
}
