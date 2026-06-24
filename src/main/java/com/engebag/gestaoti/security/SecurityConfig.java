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
                // Desabilita a proteção contra ataques CSRF (não é necessária em APIs REST com JWT)
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Define a política de sessão como Stateless (sem estado)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
             // Configura as rotas
      .authorizeHttpRequests(authorize -> authorize
                        // 1. ROTAS PÚBLICAS (Qualquer um acessa sem token)
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/esqueci-senha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/cadastrar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/redefinir-senha").permitAll()
                        .requestMatchers(HttpMethod.GET, "/departamentos").permitAll()
                        .requestMatchers("/ws-gestao/**").permitAll() 
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll() 

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()


                        .requestMatchers(HttpMethod.POST, "/chamados/*/anexos").authenticated()

                        // 2. ROTAS DO PRÓPRIO USUÁRIO (O "me" tem que vir ANTES do "{id}")
                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/me/configurar-primeiro-acesso").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/participantes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/chamados/*/participantes").authenticated()
                        // 3. ROTAS DE GESTÃO (Apenas ADMIN e TECNICO)
                        .requestMatchers(HttpMethod.POST, "/usuarios").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").hasAnyRole("ADMIN", "TECNICO")
                        .requestMatchers(HttpMethod.POST, "/usuarios/{id}/forcar-redefinicao").hasAnyRole("ADMIN", "TECNICO")
                        
                        // 4. Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )
                // Adiciona o nosso filtro personalizado ANTES do filtro padrão do Spring
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Usa o BCrypt para verificar se a senha digitada bate com o hash salvo no banco
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permite que qualquer IP ou domínio acesse a API (Ideal para desenvolvimento).
        // Em produção, você pode trocar "*" pelo IP do servidor onde o React está hospedado.
        configuration.setAllowedOriginPatterns(List.of("*")); 
        
        // Quais métodos HTTP o frontend pode usar?
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Quais cabeçalhos o frontend pode enviar? (Liberamos todos, inclusive o Authorization)
        configuration.setAllowedHeaders(List.of("*"));
        
        // Permite o envio de credenciais (como cookies ou tokens avançados)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica essa regra para TODAS as rotas da sua API (/**)
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Diz ao Spring para usar a sua classe AuthService para buscar o usuário
        provider.setUserDetailsService(userDetailsService); 
        // Diz ao Spring para usar o BCrypt para conferir a senha
        provider.setPasswordEncoder(passwordEncoder); 
        return provider;
    }
}