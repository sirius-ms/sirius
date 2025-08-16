package de.unijena.bioinf.ms.middleware.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Filter to handle exceptions thrown in follow-up filters.
 * To handle AccessDenied Exceptions, properly it has to run before any other filter.
 */
@Slf4j
public class ExceptionHandlerFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        try {
            chain.doFilter(request, response); // Pass request down the chain
        } catch (Exception ex) {
            ProblemDetail errorResponse = null;
            if (ex instanceof ResponseStatusException respException) {
                errorResponse = respException.getBody();
            } else if (ex instanceof AccessDeniedException) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                errorResponse = auth == null || !auth.isAuthenticated()
                        ? ProblemDetail.forStatus(HttpServletResponse.SC_UNAUTHORIZED)
                        : ProblemDetail.forStatus(HttpServletResponse.SC_FORBIDDEN);
                errorResponse.setTitle("Access Denied");
                errorResponse.setDetail(ex.getMessage());
            } else {
                log.error("Unexpected Error during spring security filter chain.", ex);
                errorResponse = ProblemDetail.forStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                errorResponse.setTitle("Internal Server Error");
                errorResponse.setDetail(ex.getMessage());
            }

            // Write ProblemDetail to response
            ((HttpServletResponse) response).setStatus(errorResponse.getStatus());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        }
    }
}