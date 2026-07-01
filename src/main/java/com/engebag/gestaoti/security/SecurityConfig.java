package com.engebag.gestaoti.security;
 
 
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 
@Configuration
@EnableWebSecurity
public class SecurityConfig {
 
    @Autowired
    private SecurityFilter securityFilter;
 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 1. ROTAS PÚBLICAS
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/esqueci-senha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/cadastrar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/redefinir-senha").permitAll()
                        .requestMatchers(HttpMethod.GET, "/departamentos").permitAll()
                        .requestMatchers("/ws-gestao/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
 
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
 
                        .requestMatchers(HttpMethod.POST, "/chamados/*/anexos").authenticated()
 
                        // 2. ROTAS DO PRÓPRIO USUÁRIO
                        .requestMatchers(HttpMethod.GET,   "/notificacoes").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/notificacoes/*/lida").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/me/configurar-primeiro-acesso").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/participantes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/chamados/*/participantes").authenticated()
 
                        // NOVO: ROTAS DO MURAL DE AVISOS / POSTS
                        // Cobre GET, POST, PUT, PATCH, DELETE em /avisos e /avisos/**
                        .requestMatchers("/avisos/**").authenticated()
 
                        // 3. ROTAS DE GESTÃO (Apenas ADMIN e TECNICO)
                        .requestMatchers(HttpMethod.POST, "/usuarios").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.POST, "/usuarios/{id}/forcar-redefinicao").hasAnyRole("ADMIN", "TECNICO")
 
                        // 4. Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
 
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
 
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
 
        configuration.setAllowedOriginPatterns(List.of("*"));
 
        // ALTERADO: adicionado "PATCH" — faltava e é usado por /avisos/{id}/fixar
        // e /notificacoes/{id}/lida. Sem isso o preflight do navegador falha.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
 
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
 
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
 
        return source;
    }
 
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}