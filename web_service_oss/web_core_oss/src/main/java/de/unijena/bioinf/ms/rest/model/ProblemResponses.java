package de.unijena.bioinf.ms.rest.model;

import java.net.URI;

import static de.unijena.bioinf.ms.properties.PropertyManager.getProperty;

public class ProblemResponses {
    // problem detail/response error type urls
    private static final String ERROR_TYPE_BASE_URI_KEY = "io.sirius.ms.error.base.uri";
    public static final URI ERROR_TYPE_BASE_URI = URI.create(getProperty(ERROR_TYPE_BASE_URI_KEY, null, "https://v6.docs.sirius-ms.io/errors/"));

    public static final URI ERROR_TYPE_PROJECT_VALIDATION = ERROR_TYPE_BASE_URI.resolve("/project-source-validation-failed");
    public static final URI ERROR_TYPE_IMPORT_VALIDATION = ERROR_TYPE_BASE_URI.resolve("/file-import-validation-failed");
}
