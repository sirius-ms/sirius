package de.unijena.bioinf.ms.rest.model.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AllowedFeatures(
        boolean cli,
        boolean api,
        boolean deNovo,
        boolean importMSRuns,
        boolean importPeakLists,
        boolean importCef
) {}
