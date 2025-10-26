package com.example.demo.filters;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // <-- dùng UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.services.JWTService;
import com.example.demo.services.UserServiceDTO;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTService jwtService;
    private final UserServiceDTO userServiceDTO;

    public JWTFilter(JWTService jwtService, UserServiceDTO userServiceDTO) {
        this.jwtService = jwtService;
        this.userServiceDTO = userServiceDTO;
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        
        // Skip public API endpoints
        if (path.equals("/api/auth/login") || path.startsWith("/api/public/")) {
            return true;
        }
        
        // Skip static resources and web endpoints
        return path.startsWith("/assets/")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.startsWith("/account/login")
            || path.startsWith("/account/register")
            || path.startsWith("/swagger")
            || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Nếu đã có authentication thì bỏ qua (tránh ghi đè)
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (jwtService.validToken(token)) {
                        String username = jwtService.getUsernameFromJWT(token); // thường là email
                        if (username != null && !username.isBlank()) {
                            // Tải UserDetails để lấy đầy đủ authorities (ROLE_…)
                            UserDetails userDetails = userServiceDTO.loadUserByUsername(username);
                            if (userDetails != null && userDetails.isEnabled()) {
                                UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                    );
                                authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                                );
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                log.debug("JWT authenticated user: {}", username);
                            } else {
                                log.warn("UserDetails not found or disabled for username: {}", username);
                            }
                        }
                    } else {
                        log.debug("Invalid JWT token");
                    }
                }
            }
        } catch (Exception ex) {
            // Không chặn request, chỉ log để tránh lộ thông tin
            log.error("JWT filter error: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
