package de.unijena.bioinf.ms.frontend.utils;

import picocli.CommandLine;

public class PicoUtils {
    public static CommandLine.Command getCommand(Class<?> annotatedClass) {
        return annotatedClass.getAnnotation(CommandLine.Command.class);
    }
}
