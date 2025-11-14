package com.example.demo.configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.filters.JWTFilter;
import com.example.demo.services.UserServiceDTO;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	private final UserServiceDTO accountService;
    private final JWTFilter jwtFilter;
    private final BCryptPasswordEncoder passwordEncoder;

    public SecurityConfiguration(UserServiceDTO accountService,
                                 JWTFilter jwtFilter,
                                 BCryptPasswordEncoder passwordEncoder) {
        this.accountService = accountService;
        this.jwtFilter = jwtFilter;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(accountService);
        provider.setPasswordEncoder(passwordEncoder); // dùng bean từ PasswordEncoderConfig
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** SuccessHandler cho WEB: điều hướng theo ROLE */
    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            private final Map<String, String> targetUrls = Map.of(
                "ROLE_ADMIN", "/admin/dashboard/index",
                "ROLE_STAFF", "/staff/dashboard/index",
                "ROLE_USER",  "/home/index"
            );

            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication)
                    throws IOException, ServletException {
                Set<String> roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                if (roles.contains("ROLE_ADMIN")) {
                    response.sendRedirect(targetUrls.get("ROLE_ADMIN"));
                } else if (roles.contains("ROLE_STAFF")) {
                    response.sendRedirect(targetUrls.get("ROLE_STAFF"));
                } else {
                    response.sendRedirect(targetUrls.getOrDefault("ROLE_USER", "/"));
                }
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization")); // THÊM DÒNG NÀY
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /** ===== Gộp API và WEB Security vào một chain ===== */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authenticationProvider(daoAuthProvider())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/", 
                    "/assets/**",
                    "/auth/login",
                    "/auth/register",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/artists/**",
                    "/api/songs/**",
                    "/api/import/**",
                    "/api/playlists/**",
                    "/api/audio/**",
                    "/api/audio-storage/**",
                    "/api/genres/**",
                    "/api/likes/**",
                    "/api/albums/**",
                    "/api/auth/find/**"
                ).permitAll()

                // API endpoints yêu cầu JWT
                .requestMatchers("/api/**").authenticated()

                // Role-based WEB
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(
                    "/api/auth/avatar"
                ).hasAnyRole("USER", "STAFF", "ADMIN")

                .anyRequest().authenticated()
            )
            // Session: cho phép nếu cần (API sẽ bỏ qua vì JWTFilter lo rồi)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // JWT filter cho API
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            // Form login cho WEB
//            .formLogin(form -> form
//                .loginPage("/auth/login")
//                .usernameParameter("email")
//                .passwordParameter("password")
//                .successHandler(roleBasedSuccessHandler())
//                .failureUrl("/auth/login?error")
//                .permitAll()
//            )

            // Logout cho WEB
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/account/logout"))
                .logoutSuccessUrl("/home/index")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Exception handling
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/account/access-denied")
                .authenticationEntryPoint((req, res, e) -> {
                    String path = req.getRequestURI();
                    if (path.startsWith("/api/")) {
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        res.sendRedirect("/auth/login");
                    }
                })
            );

        return http.build();
    }

}