package com.comandago.api.storage.service;

import com.comandago.api.shared.validation.ImagenUrlValidator;
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.config.SupabaseStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseStorageServiceTest {

    private SupabaseStorageService service;

    @BeforeEach
    void setUp() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.setUrl("https://test.supabase.co");
        properties.setServiceRoleKey("service-key");
        properties.getStorage().setBucketCategorias("categorias");
        properties.getStorage().setBucketProductos("productos");
        properties.getStorage().setPublicBaseUrlCategorias(
                "https://test.supabase.co/storage/v1/object/public/categorias");
        properties.getStorage().setPublicBaseUrlProductos(
                "https://test.supabase.co/storage/v1/object/public/productos");
        ImagenUrlValidator validator = new ImagenUrlValidator(properties);
        service = new SupabaseStorageService(properties, validator);
    }

    @Test
    void esUrlDelBucket_categorias_urlValida_retornaTrue() {
        String url = "https://test.supabase.co/storage/v1/object/public/categorias/1234-abc.webp";

        assertThat(service.esUrlDelBucket(StorageBucket.CATEGORIAS, url)).isTrue();
    }

    @Test
    void esUrlDelBucket_productos_urlValida_retornaTrue() {
        String url = "https://test.supabase.co/storage/v1/object/public/productos/1234-abc.webp";

        assertThat(service.esUrlDelBucket(StorageBucket.PRODUCTOS, url)).isTrue();
    }

    @Test
    void esUrlDelBucket_urlExterna_retornaFalse() {
        assertThat(service.esUrlDelBucket(StorageBucket.CATEGORIAS, "https://evil.com/imagen.webp"))
                .isFalse();
    }

    @Test
    void extraerObjectPath_categorias_urlValida_retornaNombreArchivo() {
        String url = "https://test.supabase.co/storage/v1/object/public/categorias/1234-abc.webp";

        assertThat(service.extraerObjectPath(StorageBucket.CATEGORIAS, url)).isEqualTo("1234-abc.webp");
    }

    @Test
    void extraerObjectPath_productos_urlValida_retornaNombreArchivo() {
        String url = "https://test.supabase.co/storage/v1/object/public/productos/5678-def.webp";

        assertThat(service.extraerObjectPath(StorageBucket.PRODUCTOS, url)).isEqualTo("5678-def.webp");
    }

    @Test
    void extraerObjectPath_urlExterna_retornaNull() {
        assertThat(service.extraerObjectPath(StorageBucket.CATEGORIAS, "https://evil.com/imagen.webp"))
                .isNull();
    }
}
