package com.comandago.api.storage.controller;

import com.comandago.api.shared.response.ApiResponse;
import com.comandago.api.storage.dto.StorageDeleteRequest;
import com.comandago.api.storage.dto.UploadUrlRequest;
import com.comandago.api.storage.dto.UploadUrlResponse;
import com.comandago.api.storage.service.SupabaseStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storage/productos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductosStorageController {

    private final SupabaseStorageService supabaseStorageService;

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> crearUploadUrl(
            @RequestBody(required = false) UploadUrlRequest request) {
        String extension = request != null ? request.getExtension() : "webp";
        UploadUrlResponse response = supabaseStorageService.crearUploadUrlProducto(extension);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody StorageDeleteRequest request) {
        if (request.getObjectPath() != null && !request.getObjectPath().isBlank()) {
            supabaseStorageService.eliminarObjetoProducto(request.getObjectPath());
        } else if (request.getPublicUrl() != null && !request.getPublicUrl().isBlank()) {
            supabaseStorageService.eliminarPorPublicUrlProducto(request.getPublicUrl());
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
