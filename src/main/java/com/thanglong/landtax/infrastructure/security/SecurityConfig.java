package com.thanglong.landtax.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // DÒNG QUAN TRỌNG NHẤT
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Annotation này giờ sẽ hết lỗi đỏ
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_TAX_OFFICER\n" +
                "ROLE_TAX_OFFICER > ROLE_CITIZEN\n" +
                "ROLE_ADMIN > ROLE_LAND_OFFICER\n" +
                "ROLE_LAND_OFFICER > ROLE_CITIZEN");
        return hierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Mở đường cho lệnh OPTIONS (Pre-flight) của trình duyệt
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Các API công khai
                .requestMatchers("/api/public/**", "/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/profile/sync", "/api/land-prices/lookup", "/api/lands/**", "/error").permitAll()
                // Phân quyền theo Role
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/reports/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_TAX_OFFICER", "ROLE_LAND_OFFICER")
                .requestMatchers("/api/records/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_LAND_OFFICER", "ROLE_TAX_OFFICER")
                .requestMatchers("/api/taxes/**").hasAnyAuthority("ROLE_CITIZEN", "ROLE_TAX_OFFICER", "ROLE_ADMIN")
                .requestMatchers("/api/land-parcels/**").hasAnyAuthority("ROLE_LAND_OFFICER", "ROLE_ADMIN", "ROLE_CITIZEN")
                .requestMatchers("/api/tax/**").hasAnyAuthority("ROLE_TAX_OFFICER", "ROLE_ADMIN", "ROLE_CITIZEN")
                .requestMatchers("/api/mutation-requests/**").hasAnyAuthority("ROLE_CITIZEN", "ROLE_LAND_OFFICER", "ROLE_ADMIN")
                .requestMatchers("/api/payments/**", "/api/profile/**").hasAnyAuthority("ROLE_CITIZEN", "ROLE_TAX_OFFICER", "ROLE_ADMIN")
                .requestMatchers("/api/land-prices/**").hasAnyAuthority("ROLE_LAND_OFFICER", "ROLE_ADMIN", "ROLE_TAX_OFFICER", "ROLE_CITIZEN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép Frontend ở port 3000 gọi API
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}