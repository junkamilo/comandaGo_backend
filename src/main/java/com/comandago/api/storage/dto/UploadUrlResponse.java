package com.comandago.api.storage.dto;

public record UploadUrlResponse(String signedUrl, String publicUrl, String objectPath) {
}
