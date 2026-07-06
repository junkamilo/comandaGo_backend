package com.comandago.api.storage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageDeleteRequest {

    private String objectPath;
    private String publicUrl;
}
