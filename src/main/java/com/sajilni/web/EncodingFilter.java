package com.sajilni.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Defensive: ensure UTF-8 is declared on responses that don't set it.
 */
@Component
public class EncodingFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (response instanceof HttpServletResponse resp && resp.getContentType() != null &&
                resp.getContentType().startsWith("application/json") &&
                !resp.getContentType().toLowerCase().contains("charset")) {
            resp.setContentType("application/json; charset=UTF-8");
        }
        chain.doFilter(request, response);
    }
}
