package de.unijena.bioinf.ms.middleware.security.filters;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.WebAPI;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * This filter adds the access_token and active subscription to the request in case of a locally running service.
 * This ensures that for the local szenario clients do not have to provide the token already known by the server.
 * This architecture ensures that in case this API is used as a real web service, we just have to disable this filter
 * to have proper authentication and authorization.
 * <p>
 * This filter is only active when the 'local' profile is used.
 * The @Order annotation ensures it runs before all other filters.
 */
@Component
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebApiAuthenticationProviderFilter extends OncePerRequestFilter {

    private final WebAPI<?> webAPI;

    public WebApiAuthenticationProviderFilter(WebAPI<?> webAPI) {
        this.webAPI = webAPI;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!webAPI.getAuthService().isLoggedIn()){
            filterChain.doFilter(request, response);
            return;
        }

        DefaultSubscriptionRequest wrapped = new DefaultSubscriptionRequest(request);
        webAPI.getAuthService().getToken().map(AuthService.Token::getAccessToken)
                .ifPresent(accessToken -> wrapped.addHeader("Authorization", "Bearer " + accessToken));
        Subscription sub = webAPI.getActiveSubscription();
        if (sub != null)
            wrapped.addHeader("SUBSCRIPTION", sub.getSid());

        filterChain.doFilter(wrapped, response);
    }


    public static class DefaultSubscriptionRequest extends HttpServletRequestWrapper {

        private final Map<String, String> customHeaders = new HashMap<>();

        private DefaultSubscriptionRequest(HttpServletRequest request) {
            super(request);
        }

        private void addHeader(String name, String value) {
            this.customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            // Check custom headers first, then fall back to original request
            String headerValue = customHeaders.get(name);
            if (headerValue != null)
                return headerValue;

            return ((HttpServletRequest) getRequest()).getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // Create a combined list of header names
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(customHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}