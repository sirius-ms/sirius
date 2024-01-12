package de.unijena.bioinf.lcms;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@CommandLine.Command(mixinStandardHelpOptions = true)
public class LCMSOptions {

    @CommandLine.Option(names = "--tr")
    private Path tr;

    @NotNull
    public Path getTr() {
        if (tr == null)
            return getLogDir().resolve("tr.csv");
        return tr;
    }

    @CommandLine.Option(names = "--cores", defaultValue = "-1")
    public int cores;

    @CommandLine.Option(names = "--mois")
    private Path mois;

    @NotNull
    public Path getMois() {
        if (mois == null)
            return getLogDir().resolve("mois.csv");
        return mois;
    }

    @CommandLine.Option(names = "--logDir")
    private Path logDir;

    @NotNull
    public Path getLogDir() {
        if (logDir == null)
            return Path.of(System.getProperty("user.home"));
        return logDir;
    }

    @CommandLine.Parameters(arity = "1..*")
    List<File> inputLocations;

    public List<File> getInputFiles() {
        return inputLocations.stream().flatMap(p -> {
            if (p.isDirectory())
                return Optional.ofNullable(p.listFiles()).stream().flatMap(Stream::of);
            return Stream.of(p);
        }).toList();
    }
}
