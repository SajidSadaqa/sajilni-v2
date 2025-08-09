package com.sajilni.security;

import com.sajilni.service.JwtService;
import com.sajilni.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error")
                || path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If Authorization header missing or not Bearer, just continue
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

//        try {
//            String header = request.getHeader("Authorization");
//            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
//                String token = header.substring(7);
//                if (jwtService.validateToken(token)) {
//                    String email = jwtService.getEmailFromToken(token);
//                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
//                    UsernamePasswordAuthenticationToken auth =
//                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(auth);
//                }
//            }
//        } catch (Exception ex) {
//            logger.error("Cannot set user authentication: {}", ex.getMessage());
//        }
        filterChain.doFilter(request, response);
    }
}
