package de.unijena.bioinf.ms.middleware.security.exceptions;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.security.Authorities.*;

@Order(-1)
@RestControllerAdvice
public class ExceptionErrorResponseHandler {
    public static final URI ERROR_TYPE_BASE_URI = URI.create("https://v6.docs.sirius-ms.io/errors/");

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ex.getBody();
        // Correctly extract errors using getAllErrors()
        List<FieldValidationError> validationErrors = ex.getParameterValidationResults().stream()
                .map(error -> {
                    String fieldName = error.getMethodParameter().getParameter().getName();
                    String message = error.getResolvableErrors().stream().map(MessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(" | "));
                    return FieldValidationError.of(fieldName, message, error.getArgument());
                }).collect(Collectors.toList());

        if(Utils.notNullOrEmpty(validationErrors))
            problemDetail.setProperty("validationErrors", validationErrors);
        return problemDetail;
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ProblemDetail problemDetail;
        if (auth == null || auth.getPrincipal() == null) {
            problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            problemDetail.setTitle("Not logged in");
            problemDetail.setDetail(ex.getMessage());
            if (Utils.isNullOrBlank(problemDetail.getDetail()))
                problemDetail.setDetail("You are not logged in! You have to log in to perform this action.");
            problemDetail.setType(ERROR_TYPE_BASE_URI.resolve("unauthorized"));
        } else {
            problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
            problemDetail.setTitle("Forbidden");
            problemDetail.setType(ERROR_TYPE_BASE_URI.resolve("forbidden"));

            if (!hasAuthority(ALLOWED_FEATURE__API, auth)) {
                problemDetail.setDetail("You either have no valid subscription or your active subscription does not allow API access.");
            }else {
                problemDetail.setDetail("You do not have the required permissions to perform this action.");
            }

            problemDetail.setProperty("user", auth.getName());
            problemDetail.setProperty(
                    "grantedAuthorities",
                    auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(g -> !g.startsWith(BYPASS_PREFIX)) //filter bypass prefixes because they should not be shown to public
                            .collect(Collectors.toList())
            );
        }

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}