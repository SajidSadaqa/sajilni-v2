package com.sajilni.security;

import com.sajilni.service.JwtService;
import com.sajilni.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // TODO: Using Spring Security's .permitAll() approach instead of manual path checking
        // Public endpoints should be configured in SecurityConfig with .permitAll()
        // This filter now only handles JWT extraction and validation

        try {
            String token = extractTokenFromRequest(request);

            // Only process if token exists and no authentication is already set
            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(token, request);
            }
        } catch (Exception ex) {
            // TODO: Enhanced error handling - log but don't break the filter chain
            logger.error("JWT authentication failed for request {}: {}",
                    request.getRequestURI(), ex.getMessage());
            // Clear security context on authentication failure
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX_LENGTH);
        }

        return null;
    }

    /**
     * Authenticate user based on JWT token
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        if (!jwtService.validateToken(token)) {
            logger.debug("Invalid JWT token for request: {}", request.getRequestURI());
            return;
        }

        String email = jwtService.getEmailFromToken(token);
        if (!StringUtils.hasText(email)) {
            logger.debug("No email found in JWT token");
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        logger.debug("Successfully authenticated user: {} for request: {}", email, request.getRequestURI());
    }

    /**
     * TODO: Since your SecurityConfig already handles public endpoints with .permitAll(),
     * we can remove most path checking from the filter.
     * Only skip requests that don't need JWT processing at all.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // All public endpoints are already handled by SecurityConfig .permitAll()
        // This filter should run for all requests to extract JWT when present
        // Only skip if absolutely necessary (like internal health checks)
        return false;
    }
}