package de.unijena.bioinf.ms.frontend.io;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputFiles {
    @NotNull
    public final List<Path> msParserfiles, projects, unknownFiles;

    public InputFiles() {
        this(new ArrayList<>(), new ArrayList<>(),  new ArrayList<>());
    }

    private InputFiles(@NotNull List<Path> msParserfiles, @NotNull List<Path> projects,  @NotNull List<Path> unknownFiles) {
        this.msParserfiles = msParserfiles;
        this.projects = projects;
        this.unknownFiles = unknownFiles;
    }

    public Stream<Path> getAllFilesStream() {
        return Stream.of(msParserfiles, projects, unknownFiles).flatMap(Collection::stream);
    }

    public List<Path> getAllFiles() {
        return getAllFilesStream().collect(Collectors.toList());
    }

    public File[] getAllFilesArray() {
        return getAllFilesStream().map(Path::toFile).toArray(File[]::new);
    }
}
