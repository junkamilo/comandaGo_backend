package com.comandago.api.usuario.repository;

import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Page<Usuario> findByRol(Rol rol, Pageable pageable);

    Page<Usuario> findByActivo(Boolean activo, Pageable pageable);

    Page<Usuario> findByRolAndActivo(Rol rol, Boolean activo, Pageable pageable);

    long countByRolAndActivo(Rol rol, Boolean activo);
}
