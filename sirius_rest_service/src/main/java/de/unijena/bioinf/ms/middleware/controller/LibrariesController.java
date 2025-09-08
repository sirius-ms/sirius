package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.rest.client.libraries.LibraryInfo;
import de.unijena.bioinf.webapi.WebAPI;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/libraries")
@Tag(name = "Libraries", description = "List remote libraries")
public class LibrariesController {

    private final WebAPI<?> webAPI;

    public LibrariesController(WebAPI<?> webAPI) {
        this.webAPI = webAPI;
    }

    /**
     * Get SIRIUS libraries.
     * @return list of libraries available for downloading.
     */
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public List<LibraryInfo> getLibraries() {
        try {
            return webAPI.listLibraries();
        } catch (Exception e) {
            log.error("Error getting libraries", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting libraries: " + e.getMessage(), e);
        }
    }
}
