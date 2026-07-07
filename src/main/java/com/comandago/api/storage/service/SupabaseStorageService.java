package com.comandago.api.storage.service;

import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.validation.ImagenUrlValidator;
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.config.SupabaseStorageProperties;
import com.comandago.api.storage.dto.UploadUrlResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseStorageProperties properties;
    private final ImagenUrlValidator imagenUrlValidator;
    private final RestClient restClient = RestClient.create();

    public UploadUrlResponse crearUploadUrlCategoria(String extension) {
        return crearUploadUrl(StorageBucket.CATEGORIAS, extension);
    }

    public UploadUrlResponse crearUploadUrlProducto(String extension) {
        return crearUploadUrl(StorageBucket.PRODUCTOS, extension);
    }

    public UploadUrlResponse crearUploadUrl(StorageBucket bucket, String extension) {
        validarConfiguracion();
        String ext = normalizarExtension(extension);
        String objectPath = System.currentTimeMillis() + "-" + UUID.randomUUID() + "." + ext;
        String bucketName = properties.getBucketName(bucket);

        String signPath = "/storage/v1/object/upload/sign/" + bucketName + "/" + objectPath;
        SignedUploadResponse signed = post(signPath, Map.of("upsert", false), SignedUploadResponse.class);

        if (signed == null || signed.url() == null || signed.url().isBlank()) {
            throw new BusinessException("No se pudo generar la URL de subida");
        }

        String publicUrl = construirPublicUrl(bucket, objectPath);
        String signedUrl = construirSignedUploadUrl(signed.url());
        return new UploadUrlResponse(signedUrl, publicUrl, objectPath);
    }

    public void eliminarObjetoCategoria(String objectPath) {
        eliminarObjeto(StorageBucket.CATEGORIAS, objectPath);
    }

    public void eliminarObjetoProducto(String objectPath) {
        eliminarObjeto(StorageBucket.PRODUCTOS, objectPath);
    }

    public void eliminarObjeto(StorageBucket bucket, String objectPath) {
        validarConfiguracion();
        if (objectPath == null || objectPath.isBlank()) {
            throw new BusinessException("La ruta del objeto es obligatoria");
        }
        String bucketName = properties.getBucketName(bucket);
        String deletePath = "/storage/v1/object/" + bucketName + "/" + objectPath.trim();
        delete(deletePath);
    }

    public void eliminarPorPublicUrl(StorageBucket bucket, String publicUrl) {
        if (!esUrlDelBucket(bucket, publicUrl)) {
            return;
        }
        String objectPath = extraerObjectPath(bucket, publicUrl);
        if (objectPath != null) {
            eliminarObjeto(bucket, objectPath);
        }
    }

    public void eliminarPorPublicUrlCategoria(String publicUrl) {
        eliminarPorPublicUrl(StorageBucket.CATEGORIAS, publicUrl);
    }

    public void eliminarPorPublicUrlProducto(String publicUrl) {
        eliminarPorPublicUrl(StorageBucket.PRODUCTOS, publicUrl);
    }

    public boolean esUrlDelBucket(StorageBucket bucket, String url) {
        return imagenUrlValidator.perteneceAlBucket(bucket, url);
    }

    public void validarUrlDelBucket(StorageBucket bucket, String url) {
        imagenUrlValidator.validarOpcional(bucket, url);
    }

    public String extraerObjectPath(StorageBucket bucket, String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String base = normalizarBaseUrl(properties.getPublicBaseUrl(bucket));
        String normalized = normalizarBaseUrl(publicUrl);
        if (!normalized.startsWith(base + "/")) {
            return null;
        }
        return normalized.substring(base.length() + 1);
    }

    private String construirPublicUrl(StorageBucket bucket, String objectPath) {
        String base = normalizarBaseUrl(properties.getPublicBaseUrl(bucket));
        return base + "/" + objectPath;
    }

    private String construirSignedUploadUrl(String signedPath) {
        if (signedPath == null || signedPath.isBlank()) {
            throw new BusinessException("No se pudo generar la URL de subida");
        }
        if (signedPath.startsWith("http://") || signedPath.startsWith("https://")) {
            return signedPath;
        }
        String base = properties.getUrl().replaceAll("/$", "");
        if (signedPath.startsWith("/storage/")) {
            return base + signedPath;
        }
        if (signedPath.startsWith("/object/")) {
            return base + "/storage/v1" + signedPath;
        }
        return base + "/storage/v1/" + signedPath.replaceFirst("^/+", "");
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

    private String normalizarExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "webp";
        }
        String ext = extension.trim().toLowerCase().replace(".", "");
        if (!ext.equals("webp")) {
            throw new BusinessException("Solo se permiten imágenes en formato webp");
        }
        return ext;
    }

    private void validarConfiguracion() {
        String missing = properties.missingConfigurationReason();
        if (missing != null) {
            throw new BusinessException(
                    "Supabase Storage no está configurado en el servidor (falta " + missing + "). Reinicia el backend tras editar application-local.properties.");
        }
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(URI.create(properties.getUrl().replaceAll("/$", "") + path))
                    .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                    .header("apikey", properties.getServiceRoleKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("Error Supabase POST {}: {}", path, responseBody);
            if (ex.getStatusCode().value() == 404 && responseBody.contains("related resource does not exist")) {
                throw new BusinessException(
                        "El bucket de Supabase Storage no existe. Crea los buckets 'categorias' y 'productos' en Supabase (ver docs/supabase-storage-setup.md).");
            }
            throw new BusinessException("Error al comunicarse con el almacenamiento de imágenes");
        }
    }

    private void delete(String path) {
        try {
            restClient.delete()
                    .uri(URI.create(properties.getUrl().replaceAll("/$", "") + path))
                    .header("Authorization", "Bearer " + properties.getServiceRoleKey())
                    .header("apikey", properties.getServiceRoleKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Error Supabase DELETE {}: {}", path, ex.getResponseBodyAsString());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SignedUploadResponse(@JsonProperty("url") String url, @JsonProperty("token") String token) {
    }
}
