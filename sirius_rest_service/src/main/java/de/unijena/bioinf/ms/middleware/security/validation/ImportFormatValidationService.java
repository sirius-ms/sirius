package de.unijena.bioinf.ms.middleware.security.validation;

import de.unijena.bioinf.babelms.MsExperimentParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.security.exceptions.ExceptionErrorResponseHandler.ERROR_TYPE_BASE_URI;
import static de.unijena.bioinf.ms.middleware.security.Authorities.*;

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

        boolean isMSRuns = hasAuthority(ALLOWED_FEATURE__IMPORT_MSRUNS, auth);
        boolean isPeakList = hasAuthority(ALLOWED_FEATURE__IMPORT_PEAKLISTS, auth);
        boolean isCef = hasAuthority(ALLOWED_FEATURE__IMPORT_CEF, auth);

        List<String> forbiddenFiles = files.stream()
                .filter(filename ->
                        (!isMSRuns && MsExperimentParser.isLCMSFile(filename)) ||
                        (!isCef && MsExperimentParser.isCefFile(filename)) ||
                        (!isPeakList && MsExperimentParser.isPeakListFile(filename) && !MsExperimentParser.isCefFile(filename)))
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
            ex.getBody().setProperty("allowedFormats", extractPermittedFiles(isMSRuns, isPeakList, isCef));
            throw ex;
        }
    }

    private Set<String> extractPermittedFiles(boolean isMsRuns, boolean isPeakList, boolean isCef) {
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
