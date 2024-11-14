package de.unijena.bioinf.ms.persistence.storage.exceptions;

import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import org.jetbrains.annotations.NotNull;

public class ProjectTypeException extends IllegalArgumentException {
    public ProjectTypeException(@NotNull ProjectType expected, @NotNull ProjectType actual) {
        super(suffix(expected, actual));
    }

    public ProjectTypeException(String message, @NotNull ProjectType expected, @NotNull ProjectType actual) {
        super(suffix(expected, actual, message));
    }

    public ProjectTypeException(String message, @NotNull ProjectType expected, @NotNull ProjectType actual, Throwable cause) {
        super(suffix(expected, actual, message), cause);
    }

    public ProjectTypeException(@NotNull ProjectType expected, @NotNull ProjectType actual, Throwable cause) {
        super(suffix(expected, actual), cause);
    }

    private static String suffix(ProjectType expected, ProjectType actual){
        return String.format("Incompatible project type. Expected: %s, Actual: %s", expected, actual);
    }

    private static String suffix(ProjectType expected, ProjectType actual, String message){
        if (message == null || message.isBlank())
            return suffix(expected, actual);
        return message  + " | " + suffix(expected, actual) ;
    }
}
