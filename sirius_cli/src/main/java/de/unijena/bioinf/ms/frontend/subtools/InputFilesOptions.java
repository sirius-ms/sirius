package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputFilesOptions {

    public Stream<Path> getAllFilesStream() {
        if (msInput == null)
            return Stream.of();
        return Stream.of(msInput.msParserfiles, msInput.projects, msInput.unknownFiles).flatMap(Collection::stream);
    }

    public List<Path> getAllFiles() {
        return getAllFilesStream().collect(Collectors.toList());
    }

    public File[] getAllFilesArray() {
        return getAllFilesStream().map(Path::toFile).toArray(File[]::new);
    }

    @CommandLine.ArgGroup(exclusive = false, heading = "Multi compound inputs (.ms, .mgf, .mzML/.mzXml, .sirius): %n")
    @Nullable
    public /*final*/ MsInput msInput;


    public static class MsInput {
        public final List<Path> msParserfiles, projects, unknownFiles;

        public MsInput() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public MsInput(List<Path> msParserfiles, List<Path> projects, List<Path> unknownFiles) {
            this.msParserfiles = msParserfiles;
            this.projects = projects;
            this.unknownFiles = unknownFiles;
        }

        @CommandLine.Option(names = {"--input", "-i"}, description = "Specify multi compound Input formats. Ths can be either preprocessed mass spectra in .ms or .mgf file format, " +
                "LC/MS runs in .mzML/.mzXml format or already existing SIRIUS project-space(s) (uncompressed/compressed).", required = true, split = ",")
        protected void setMsInputFiles(List<File> files) {
            msParserfiles.clear();
            projects.clear();
            unknownFiles.clear();
            InstanceImporter.expandInputFromFile(files, this);
        }

        @CommandLine.Option(names = {"--ignore-formula"}, description = "ignore given molecular formula if present in .ms or .mgf input files.", defaultValue = "false")
        public boolean ignoreFormula;
    }

    // region Options: CSV Input
    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Generic per compound based inputs (CSV):%n")
    @Nullable
    public /*final*/ List<CsvInput> csvInputs;

    public static class CsvInput {
        @CommandLine.Option(names = {"-1", "--ms1"}, description = "MS1 spectra file names", split = ",")
        public List<File> ms1;

        @CommandLine.Option(names = {"-2", "--ms2"}, description = "MS2 spectra file names", required = true, split = ",")
        public List<File> ms2;

        @CommandLine.Option(names = {"-z", "--parentmass", "--precursor", "--mz"}, description = "the mass of the parent ion for specified ms2 spectra", required = true)
        public Double parentMz;

        @CommandLine.Option(names = {"--adduct"}, description = "Specify the adduct for the corresponding Compound", defaultValue = "[M+?]+")
        protected void setAdduct(String adduct) {
            this.adduct = PrecursorIonType.fromString(adduct);
        }

        public PrecursorIonType adduct;
    }
}
