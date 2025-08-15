package de.unijena.bioinf.ms.middleware.security.validation;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectSourceFormats;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.security.Authorities.*;
import static de.unijena.bioinf.ms.rest.model.ProblemResponses.ERROR_TYPE_BASE_URI;

/**
 * Service to validate whether input files to be imported are permitted by the active Subscription.
 */
@Component
public class ImportFormatValidationService {

    public void validateFiles(HttpServletRequest request, MultipartFile[] files) {
        validateFiles(request, Arrays.stream(files).map(MultipartFile::getOriginalFilename).toList());
    }

    public void validateFiles(HttpServletRequest request, String[] filePaths) {
        validateFiles(request, Arrays.asList(filePaths));
    }

    public void validateFiles(HttpServletRequest request, List<String> files) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null)
            throw new AccessDeniedException("Not logged in! You need to be logged in to import files.");

        List<String> unsupportedFiles = files.stream()
                .filter(filename -> !MsExperimentParser.isSupportedFileName(filename))
                .toList();

        Set<String> permittedFormats = extractPermittedFormats(auth);

        List<String> forbiddenFiles = files.stream()
                .filter(filename -> !permittedFormats.contains(FileUtils.getFileExtOpt(filename).map(String::toLowerCase).orElse(null)))
                .toList();

        if (!unsupportedFiles.isEmpty() || !forbiddenFiles.isEmpty()) {
            ErrorResponseException ex = new ErrorResponseException(HttpStatus.BAD_REQUEST);
            ex.setTitle("File import validation failed");
            ex.setDetail(String.format("Invalid file formats! Some file formats in your input are either not supported%sor not permitted by your subscription%s.",
                    unsupportedFiles.isEmpty() ? " " : " (" + unsupportedFiles.stream().map(f -> f.substring(f.lastIndexOf('.'))).distinct().collect(Collectors.joining(", ")) + ") ",
                    forbiddenFiles.isEmpty() ? " " : " (" + forbiddenFiles.stream().map(f -> f.substring(f.lastIndexOf('.'))).distinct().collect(Collectors.joining(", ")) + ") "
            ));
            ex.setType(ERROR_TYPE_BASE_URI.resolve("/file-import-validation-failed"));
            ex.setInstance(URI.create(request.getRequestURI()));

            if (!unsupportedFiles.isEmpty())
                ex.getBody().setProperty("unsupportedFiles", unsupportedFiles);
            if (!forbiddenFiles.isEmpty())
                ex.getBody().setProperty("forbiddenFiles", forbiddenFiles);
            ex.getBody().setProperty("supportedFormats", MsExperimentParser.getAllEndings());
            ex.getBody().setProperty("allowedFormats", permittedFormats);
            throw ex;
        }
    }

    public void validateProject(HttpServletRequest request, Project<?> project) {
        validateProject(request, project, false);
    }

    public void validateProject(HttpServletRequest request, Project<?> project, boolean allowGuiBypass) {
        ProjectSourceFormats sourceFormats = project.getProjectSourceFormats().orElse(null);

        if (sourceFormats != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // only check for API support if api support not forbidden
            if (allowGuiBypass && hasAuthority(BYPASS__GUI, auth))
                return;

            Set<String> permittedFormats = extractPermittedFormats(auth);
            List<String> forbiddenFormats = sourceFormats.getFormats().stream()
                    .filter(f -> !permittedFormats.contains(f.toLowerCase()))
                    .toList();

            if (!forbiddenFormats.isEmpty()) {
                ErrorResponseException ex = makeProjectValidationException(request, project);
                ex.setTitle("Illegal project source format");
                ex.setDetail(String.format("The analysis of projects created from '%s' is not supported by your active subscription. Change to a different subscription or contact support.", String.join(",", forbiddenFormats)));
                ex.getBody().setProperty("allowedSourceFormats", permittedFormats);
                throw ex;
            }

            if (!hasAuthority(ALLOWED_FEATURE__API, auth)) {
                Set<String> permittedDirectImports = new HashSet<>();

                if (hasAuthority(BYPASS__EXPLORER, auth))
                    permittedDirectImports.add(ProjectSourceFormats.EXPLORER_DIRECT_IMPORT);

                if (sourceFormats.getDirectImports().stream().anyMatch(f -> !permittedDirectImports.contains(f))) {
                    ErrorResponseException ex = makeProjectValidationException(request, project);
                    ex.setTitle("API projects not Permitted");
                    ex.setDetail("Your subscription does not support computations on projects generated via API (direct import). Change to a subscription with API support or contact support.");
                    ex.getBody().setProperty("allowedSourceFormats", permittedFormats);
                    ex.getBody().setProperty("allowedDirectImports", permittedDirectImports);
                }
            }
        }
    }

    private static ErrorResponseException makeProjectValidationException(HttpServletRequest request, Project<?> project) {
        ErrorResponseException ex = new ErrorResponseException(HttpStatus.FORBIDDEN);
        ex.setType(ERROR_TYPE_BASE_URI.resolve("/project-source-validation-failed"));
        ex.setInstance(URI.create(request.getRequestURI()));
        ex.getBody().setProperty("projectId", project.getProjectId());
        return ex;
    }


    private Set<String> extractPermittedFormats(Authentication auth) {
        boolean isMsRuns = hasAuthority(ALLOWED_FEATURE__IMPORT_MSRUNS, auth);
        boolean isPeakList = hasAuthority(ALLOWED_FEATURE__IMPORT_PEAKLISTS, auth);
        boolean isCef = hasAuthority(ALLOWED_FEATURE__IMPORT_CEF, auth);

        Set<String> supportedFormats = new LinkedHashSet<>();
        if (isMsRuns)
            supportedFormats.addAll(MsExperimentParser.getLCMSEndings());
        if (isPeakList)
            supportedFormats.addAll(MsExperimentParser.getPeakListEndings());
        if (isCef)
            supportedFormats.add(".cef");
        else
            supportedFormats.remove(".cef");

        return supportedFormats;
    }
}
