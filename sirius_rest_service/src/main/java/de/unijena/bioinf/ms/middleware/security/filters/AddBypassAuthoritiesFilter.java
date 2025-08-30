package de.unijena.bioinf.ms.middleware.security.filters;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Filter to add authorities to authentication based on bypass rules.
 */
public class AddBypassAuthoritiesFilter extends OncePerRequestFilter {

    private final @NotNull List<Function<HttpServletRequest, Collection<GrantedAuthority>>> bypasses;

    @SafeVarargs
    public AddBypassAuthoritiesFilter(@NotNull Function<HttpServletRequest, Collection<GrantedAuthority>>... bypasses) {
        this(Arrays.asList(bypasses));
    }

    public AddBypassAuthoritiesFilter(@NotNull List<Function<HttpServletRequest, Collection<GrantedAuthority>>> bypasses) {
        this.bypasses = bypasses;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        List<GrantedAuthority> newAuthorities =  bypasses.stream().filter(Objects::nonNull)
                .map(bypass -> bypass.apply(request)).filter(Objects::nonNull)
                .flatMap(Collection::stream).collect(Collectors.toList());

        if (Utils.notNullOrEmpty(newAuthorities)){
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null)
                newAuthorities.addAll(auth.getAuthorities());
            if (auth instanceof JwtAuthenticationToken subAuthToken) {
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(subAuthToken.getToken(), newAuthorities));
            }else {
                SecurityContextHolder.getContext().setAuthentication(new BypassAuthentication(newAuthorities));
            }
        }

        filterChain.doFilter(request, response);
    }

    public static class BypassAuthentication implements Authentication {

        private boolean authenticated;
        private final List<GrantedAuthority> authorities;

        private BypassAuthentication(GrantedAuthority... authorities) {
            this.authenticated = true;
            this.authorities = authorities == null ? List.of() : Arrays.asList(authorities);
        }
        private BypassAuthentication(List<GrantedAuthority> authorities) {
            this.authenticated = true;
            this.authorities = authorities == null ? List.of() : List.copyOf(authorities);
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public Object getCredentials() {
            return null; // No credentials
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return null;
        }

        @Override
        public boolean isAuthenticated() {
            return this.authenticated;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            this.authenticated = isAuthenticated;
        }

        @Override
        public String getName() {
            return "Allowed Features";
        }
    }
}
