package de.unijena.bioinf.ms.middleware.security;

import de.unijena.bioinf.ms.middleware.ErrorResponseHandler;
import de.unijena.bioinf.ms.rest.model.ProblemResponse;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.webapi.rest.RestAPI;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;


public class ApiAllowedFilter extends OncePerRequestFilter {

    private final List<String> includeAntPaths;
    private final List<String> excludeAntPaths;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ErrorResponseHandler errorResponseHandler;
    RestAPI restService;

    @Nullable Predicate<HttpServletRequest> bypassRule;

    public ApiAllowedFilter(@Nullable Predicate<HttpServletRequest> bypassRule, ErrorResponseHandler errorResponseHandler, List<String> includeAntPaths, List<String> excludeAntPaths) {
        this.includeAntPaths = includeAntPaths;
        this.excludeAntPaths = excludeAntPaths;
        this.errorResponseHandler = errorResponseHandler;
        this.bypassRule = bypassRule;
    }

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String requestPath = request.getServletPath();
        return includeAntPaths.stream().noneMatch(p -> matcher.match(p, requestPath))
                || excludeAntPaths.stream().anyMatch(p -> matcher.match(p, requestPath));
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest req, @NotNull HttpServletResponse resp, @NotNull FilterChain chain) throws IOException, ServletException {

        if (bypassRule == null || !bypassRule.test(req)) {

            if (restService.getAuthService().isLoggedIn()) {
                errorResponseHandler.sendError(req, resp, HttpStatus.FORBIDDEN, "Not logged in!", "You are not logged in. Please login to use the SIRIUS API.");
                return;
            }

            Subscription sub = restService.getActiveSubscription();
            if (sub == null) {
                errorResponseHandler.sendError(req, resp, HttpStatus.FORBIDDEN, "No active subscription!", "No active subsciprion found. Please refresh you login token, select a proper subscription, or re-login.");
                return;
            }

            if (sub.isExpired()) {
                errorResponseHandler.sendError(req, resp, HttpStatus.FORBIDDEN, "Subscription expired!", "Subscription expired. Please renew your subscription or select a different non-expired subscription.", ProblemResponse.SUB_EXPIRED);
                return;
            }

            if (sub.getAllowedFeatures() == null) {
                errorResponseHandler.sendError(req, resp, HttpStatus.FORBIDDEN, "Allowed feature data missing!", "Allowed feature data missing. This is likely a BUG. Please contact support!", null);
                return;
            }

            if (!sub.getAllowedFeatures().api()) {
                errorResponseHandler.sendError(req, resp, HttpStatus.FORBIDDEN, "API access not allowed!", "You License/Subscription does not contain API functionality. Please upgrade your subscription to use the SIRIUS API!", null);
                return;
            }

            //todo remove. jsut for checking if all bypasses work.
            errorResponseHandler.sendError(req, resp, HttpStatus.FORBIDDEN, "No Bypass!", "This is a DEBUG check failing request where the bypass mechanism does not work! Request: " + req.getRequestURI());
        }

        chain.doFilter(req, resp);
    }


}
