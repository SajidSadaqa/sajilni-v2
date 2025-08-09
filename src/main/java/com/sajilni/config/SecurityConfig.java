package com.sajilni.config;

import com.sajilni.security.JwtAuthenticationEntryPoint;
import com.sajilni.security.JwtAuthenticationFilter;
import com.sajilni.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint entryPoint;
    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsServiceImpl userDetails;

    public SecurityConfig(JwtAuthenticationEntryPoint entryPoint,
                          JwtAuthenticationFilter jwtFilter,
                          UserDetailsServiceImpl userDetails) {
                this.entryPoint = entryPoint;
                this.jwtFilter = jwtFilter;
                this.userDetails = userDetails;
            }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Swagger & health
                        .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/actuator/health",
                        "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetails);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Dev CORS: open up to Swagger UI at http://localhost:8080 (same origin) and your React app (adjust origins)
   @Bean
   public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern("*"); // for dev; restrict in prod
        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");
        cfg.setAllowCredentials(false); // easier for dev; set true only with specific origins
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
   }
}
