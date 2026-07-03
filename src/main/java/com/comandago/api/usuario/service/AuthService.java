package com.comandago.api.usuario.service;

import com.comandago.api.shared.config.JwtService;
import com.comandago.api.shared.config.LoginRateLimiter;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.shared.exception.UnauthorizedException;
import com.comandago.api.shared.security.SecurityUtils;
import com.comandago.api.shared.security.UsuarioPrincipal;
import com.comandago.api.usuario.dto.request.AuthRequest;
import com.comandago.api.usuario.dto.request.RegisterRequest;
import com.comandago.api.usuario.dto.request.LogoutRequest;
import com.comandago.api.usuario.dto.request.RefreshTokenRequest;
import com.comandago.api.usuario.dto.request.UsuarioPasswordChangeRequest;
import com.comandago.api.usuario.dto.response.AuthResponse;
import com.comandago.api.usuario.dto.response.RolResponse;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.repository.UsuarioRepository;
import com.comandago.api.usuario.dto.mapper.UsuarioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AuthResponse register(RegisterRequest request) {
        String clientKey = "register:" + resolveClientKey();
        loginRateLimiter.checkAllowed(clientKey);

        if (usuarioRepository.count() > 0) {
            loginRateLimiter.recordFailedAttempt(clientKey);
            throw new ConflictException(
                    "El registro inicial ya fue realizado. Use login o solicite acceso al administrador.");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            loginRateLimiter.recordFailedAttempt(clientKey);
            throw new ConflictException("Ya existe un usuario con ese email");
        }

        String hash = passwordEncoder.encode(request.getPassword());
        Usuario usuario = usuarioMapper.toBootstrapAdmin(request, hash);
        usuario = usuarioRepository.save(usuario);

        loginRateLimiter.reset(clientKey);

        TokenService.TokenPair tokens = tokenService.issueTokens(usuario);
        return toAuthResponse(usuario, tokens);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String clientKey = resolveClientKey();
        loginRateLimiter.checkAllowed(clientKey);

        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException ex) {
            loginRateLimiter.recordFailedAttempt(clientKey);
            throw new UnauthorizedException("Credenciales inválidas");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    loginRateLimiter.recordFailedAttempt(clientKey);
                    return new UnauthorizedException("Credenciales inválidas");
                });

        loginRateLimiter.reset(clientKey);

        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        TokenService.TokenPair tokens = tokenService.issueTokens(usuario);
        return toAuthResponse(usuario, tokens);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        TokenService.TokenPair tokens = tokenService.refresh(request.getRefreshToken());
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(jwtService.extractEmail(tokens.accessToken()))
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        return toAuthResponse(usuario, tokens);
    }

    @Transactional
    public void logout(String authorizationHeader, LogoutRequest request) {
        String accessToken = extractBearer(authorizationHeader);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        tokenService.logout(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me() {
        UsuarioPrincipal principal = SecurityUtils.currentUser();
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public void cambiarPasswordPropia(UsuarioPasswordChangeRequest request) {
        UsuarioPrincipal principal = SecurityUtils.currentUser();
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new BusinessException("La contraseña actual no es correcta");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);
        tokenService.revokeAllRefreshForUser(usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return Arrays.stream(Rol.values()).map(RolResponse::from).toList();
    }

    private AuthResponse toAuthResponse(Usuario usuario, TokenService.TokenPair tokens) {
        return AuthResponse.builder()
                .token(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresIn(tokens.expiresInSeconds())
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .build();
    }

    private String extractBearer(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private String resolveClientKey() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
