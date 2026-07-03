package com.comandago.api.usuario.service;

import com.comandago.api.shared.config.JwtService;
import com.comandago.api.shared.config.SecurityProperties;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.UnauthorizedException;
import com.comandago.api.usuario.entity.RefreshToken;
import com.comandago.api.usuario.entity.RevokedJwt;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.repository.RefreshTokenRepository;
import com.comandago.api.usuario.repository.RevokedJwtRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedJwtRepository revokedJwtRepository;

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}

    @Transactional
    public TokenPair issueTokens(Usuario usuario) {
        String accessToken = jwtService.generateToken(usuario.getEmail(), Map.of(
                "id", usuario.getId(),
                "rol", usuario.getRol().name(),
                "nombre", usuario.getNombre()));
        String refreshToken = createRefreshToken(usuario);
        return new TokenPair(accessToken, refreshToken, jwtService.getExpirationMs() / 1000);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Refresh token expirado");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        Usuario usuario = stored.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UnauthorizedException("Usuario inactivo");
        }

        return issueTokens(usuario);
    }

    @Transactional
    public void revokeAllRefreshForUser(Long usuarioId) {
        refreshTokenRepository.revokeAllByUsuarioId(usuarioId);
    }

    @Transactional
    public void logout(String accessToken, String rawRefreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtService.parseToken(accessToken);
                String jti = claims.getId();
                if (jti != null) {
                    OffsetDateTime expiresAt = claims.getExpiration().toInstant()
                            .atOffset(OffsetDateTime.now().getOffset());
                    revokedJwtRepository.save(RevokedJwt.builder()
                            .jti(jti)
                            .expiresAt(expiresAt)
                            .build());
                }
            } catch (Exception ignored) {
                // token ya inválido
            }
        }
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.revokeByTokenHash(hashToken(rawRefreshToken));
        }
    }

    @Transactional(readOnly = true)
    public boolean isAccessTokenRevoked(String jti) {
        return jti != null && revokedJwtRepository.existsByJti(jti);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredRevokedTokens() {
        revokedJwtRepository.deleteExpiredBefore(OffsetDateTime.now());
    }

    private String createRefreshToken(Usuario usuario) {
        String raw = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hashToken(raw))
                .expiresAt(OffsetDateTime.now().plusSeconds(securityProperties.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    private String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("Error al procesar token");
        }
    }
}
