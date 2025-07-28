package de.unijena.bioinf.ms.middleware.security.filters;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.middleware.security.Authorities;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import io.sirius.ms.utils.jwt.AccessTokens;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Add authorities from active Subscription to authentication
 */
public class AddFeatureAuthoritiesFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String subId = request.getHeader("SUBSCRIPTION");
            if (Utils.notNullOrBlank(subId)) {
                try {
                    Subscription subscription = AccessTokens.ACCESS_TOKENS.getActiveSubscription(auth.getPrincipal(), subId);
                    if (subscription !=  null && !subscription.isExpired()){
                        List<GrantedAuthority> newAuthorities = new ArrayList<>(auth.getAuthorities());
                        newAuthorities.addAll(Authorities.getFromSubscription(subscription));
                        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken((Jwt) auth.getPrincipal(), newAuthorities));
                    }
                } catch (Exception e) {
                    logger.error("Unsupported Authentication type. could not extract token. Or not logged in in but subscription given.", e);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
