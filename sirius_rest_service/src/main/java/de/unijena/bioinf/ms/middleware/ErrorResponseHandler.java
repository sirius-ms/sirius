package de.unijena.bioinf.ms.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.rest.model.ProblemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class ErrorResponseHandler {

    private final ObjectMapper objectMapper;

    public ErrorResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendError(@NotNull HttpServletResponse response, @NotNull ResponseStatusException exception) {
        sendError(null, response, exception);
    }

    public void sendError(@Nullable HttpServletRequest req, @NotNull HttpServletResponse response, @NotNull ResponseStatusException exception) {
        sendError(req, response, exception.getStatusCode(), null, exception.getReason(), null);
    }

    public void sendError(@NotNull HttpServletResponse response, @NotNull HttpStatusCode status) {
        sendError(response, status, null);
    }

    public void sendError(@NotNull HttpServletResponse response, @NotNull HttpStatusCode status, String message) {
        sendError(response, status, null, message);
    }

    public void sendError(@NotNull HttpServletResponse response, @NotNull HttpStatusCode status, @Nullable String title, @Nullable String message) {
        sendError(null, response, status, title, message, null);
    }

    public void sendError(@NotNull HttpServletResponse response, int status, @NotNull String title, @Nullable String message) {
        sendError(null, response, status, title, message, null);
    }

    public void sendError(@Nullable HttpServletRequest req, @NotNull HttpServletResponse response, @NotNull HttpStatusCode status, @Nullable String title, @Nullable String message) {
        sendError(req, response, status, title, message,null);
    }

    public void sendError(@Nullable HttpServletRequest req, @NotNull HttpServletResponse response, @NotNull HttpStatusCode status, @Nullable String title, @Nullable String message, @Nullable String errorCode) {
        sendError(req, response, status.value(), title == null || title.isBlank() ? status.toString() : title, message, errorCode);
    }

    public void sendError(@Nullable HttpServletRequest req, @NotNull HttpServletResponse response, int status, @NotNull String title, @Nullable String message, @Nullable String errorCode) {
        try {

            ProblemResponse.ProblemResponseBuilder bodyBuilder =
                    ProblemResponse.builder()
                            .status(status)
                            .title(title)
                            .detail(message)
                            .errorCode(errorCode);

            if (req != null)
                bodyBuilder.instance(req.getRequestURI());

            ProblemResponse body = bodyBuilder.build();

            response.setStatus(status);
            response.setContentType("application/problem+json");

            try (OutputStream s = response.getOutputStream()) {
                objectMapper.writeValue(s, body);
            }
        } catch (IOException e) {
            response.setStatus(status);
        }
    }
}
