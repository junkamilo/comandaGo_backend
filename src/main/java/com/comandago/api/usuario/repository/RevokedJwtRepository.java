package com.comandago.api.usuario.repository;

import com.comandago.api.usuario.entity.RevokedJwt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;

public interface RevokedJwtRepository extends JpaRepository<RevokedJwt, String> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM RevokedJwt r WHERE r.expiresAt < :before")
    void deleteExpiredBefore(OffsetDateTime before);
}
