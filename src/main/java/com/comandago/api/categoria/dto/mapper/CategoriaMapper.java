package com.comandago.api.categoria.dto.mapper;

import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.request.CategoriaUpdateRequest;
import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.categoria.entity.Categoria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoriaMapper {

    public Categoria toEntity(CategoriaCreateRequest request) {
        return Categoria.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .imagenUrl(request.getImagenUrl())
                .orden(request.getOrden())
                .activo(true)
                .build();
    }

    public void updateEntity(Categoria categoria, CategoriaUpdateRequest request) {
        if (request.getNombre() != null) {
            categoria.setNombre(request.getNombre());
        }
        if (request.getDescripcion() != null) {
            categoria.setDescripcion(request.getDescripcion());
        }
        if (request.getImagenUrl() != null) {
            categoria.setImagenUrl(request.getImagenUrl());
        }
        if (request.getOrden() != null) {
            categoria.setOrden(request.getOrden());
        }
    }

    public CategoriaResponse toResponse(Categoria categoria) {
        Categoria padre = categoria.getCategoriaPadre();
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .imagenUrl(categoria.getImagenUrl())
                .orden(categoria.getOrden())
                .activo(categoria.getActivo())
                .categoriaPadreId(padre != null ? padre.getId() : null)
                .categoriaPadreNombre(padre != null ? padre.getNombre() : null)
                .subcategorias(List.of())
                .build();
    }

    public CategoriaResponse toResponseConHijas(Categoria padre, List<Categoria> hijas) {
        Categoria abuelo = padre.getCategoriaPadre();
        return CategoriaResponse.builder()
                .id(padre.getId())
                .nombre(padre.getNombre())
                .descripcion(padre.getDescripcion())
                .imagenUrl(padre.getImagenUrl())
                .orden(padre.getOrden())
                .activo(padre.getActivo())
                .categoriaPadreId(abuelo != null ? abuelo.getId() : null)
                .categoriaPadreNombre(abuelo != null ? abuelo.getNombre() : null)
                .subcategorias(hijas.stream().map(this::toResponse).toList())
                .build();
    }
}
