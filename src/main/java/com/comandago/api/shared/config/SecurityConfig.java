package com.comandago.api.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/me/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/roles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/menu").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/promociones").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/promociones/vigentes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/menu-del-dia")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias/menu").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/mesas/qr/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos").permitAll()
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categorias/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/storage/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias/**")
                        .hasAnyRole("ADMIN", "MESERO", "COCINERO", "CAJERO", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/productos/*/disponibilidad")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/promociones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/promociones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/promociones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/promociones/**")
                        .hasAnyRole("ADMIN", "MESERO", "COCINERO", "CAJERO", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**")
                        .hasAnyRole("ADMIN", "MESERO", "COCINERO", "CAJERO", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/mesas/piso")
                        .hasAnyRole("ADMIN", "MESERO", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/mesas/libres")
                        .hasAnyRole("ADMIN", "MESERO", "CAJERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/mesas/agrupar")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/mesas/grupo/*")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/mesas/**")
                        .hasAnyRole("ADMIN", "MESERO", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/mesas/*/estado")
                        .hasAnyRole("ADMIN", "MESERO", "RECEPCIONISTA")
                        .requestMatchers("/api/v1/mesas/**").hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/cocina").hasAnyRole("ADMIN", "COCINERO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/activos")
                        .hasAnyRole("ADMIN", "MESERO", "COCINERO", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/mesa/*")
                        .hasAnyRole("ADMIN", "MESERO", "CAJERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/estado")
                        .hasAnyRole("ADMIN", "MESERO", "COCINERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/entregar")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/cancelar")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/cancelar-detalles")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/*/detalles/lote")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pedidos/*/detalles/*/reemplazar")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/detalles/*/entregar")
                        .hasAnyRole("ADMIN", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/detalles/*/estado")
                        .hasAnyRole("ADMIN", "COCINERO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pagos/pedido/*")
                        .hasAnyRole("ADMIN", "CAJERO", "MESERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pagos")
                        .hasAnyRole("ADMIN", "CAJERO", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pagos/*/confirmar")
                        .hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pagos/*/rechazar")
                        .hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pagos/*/reembolsar")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/pagos/**").hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/caja/preview")
                        .hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/caja/cerrar")
                        .hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers("/api/v1/caja/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/pedidos/**")
                        .hasAnyRole("ADMIN", "MESERO", "COCINERO", "CAJERO", "RECEPCIONISTA")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
