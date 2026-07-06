package com.comandago.api.shared.validation;

import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.config.SupabaseStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenUrlValidatorTest {

    private ImagenUrlValidator validator;

    @BeforeEach
    void setUp() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.getStorage().setPublicBaseUrlCategorias(
                "https://test.supabase.co/storage/v1/object/public/categorias");
        properties.getStorage().setPublicBaseUrlProductos(
                "https://test.supabase.co/storage/v1/object/public/productos");
        validator = new ImagenUrlValidator(properties);
    }

    @Test
    void validar_urlDelBucketCategorias_noLanzaExcepcion() {
        validator.validar(
                StorageBucket.CATEGORIAS,
                "https://test.supabase.co/storage/v1/object/public/categorias/123.webp");
    }

    @Test
    void validar_urlExterna_lanzaBusinessException() {
        assertThatThrownBy(() -> validator.validar(StorageBucket.CATEGORIAS, "https://evil.com/x.webp"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("almacenamiento autorizado");
    }

    @Test
    void validar_extensionNoWebp_lanzaBusinessException() {
        assertThatThrownBy(() -> validator.validar(
                        StorageBucket.PRODUCTOS,
                        "https://test.supabase.co/storage/v1/object/public/productos/foto.jpg"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WebP");
    }

    @Test
    void validarOpcional_nullNoLanzaExcepcion() {
        validator.validarOpcional(StorageBucket.CATEGORIAS, null);
        validator.validarOpcional(StorageBucket.CATEGORIAS, "   ");
    }

    @Test
    void perteneceAlBucket_urlValida_retornaTrue() {
        assertThat(validator.perteneceAlBucket(
                        StorageBucket.PRODUCTOS,
                        "https://test.supabase.co/storage/v1/object/public/productos/a.webp"))
                .isTrue();
    }

    @Test
    void validar_urlMuyLarga_lanzaBusinessException() {
        String base = "https://test.supabase.co/storage/v1/object/public/categorias/";
        String larga = base + "a".repeat(300) + ".webp";

        assertThatThrownBy(() -> validator.validar(StorageBucket.CATEGORIAS, larga))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("255 caracteres");
    }
}
