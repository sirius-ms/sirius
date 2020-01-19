package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

    @CommandLine.ArgGroup(exclusive = false, heading = "Multi compound inputs (.ms, .mgf, .mzML/.mzXml, .sirius): %n", order = 120)
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

        @CommandLine.Option(names = {"--input", "-i"}, description = "Specify input data. This can be multi compound Input formats: Preprocessed mass spectra in .ms or .mgf file format, " +
                "LC/MS runs in .mzML/.mzXml format or already existing SIRIUS project-spaces (uncompressed/compressed) but also any other file type e.g. to provide input for STANDALONE tools.", required = true, split = ",", order = 121)
        protected void setInputPath(List<Path> files) {
            msParserfiles.clear();
            projects.clear();
            unknownFiles.clear();
            InstanceImporter.expandInput(files, this);
        }

        @CommandLine.Option(names = {"--ignore-formula"}, description = "ignore given molecular formula if present in .ms or .mgf input files.", defaultValue = "false", order = 122)
        public boolean ignoreFormula;
    }

    // region Options: CSV Input
    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "Generic per compound based inputs (CSV):%n", order = 130)
    public List<CsvInput> csvInputs;

    public static class CsvInput {
        @CommandLine.Option(names = {"-1", "--ms1"}, description = "MS1 spectra file names", paramLabel = "<ms1File>[,<ms1File>...]", order = 131)
        protected void setMs1(String ms1Files){
            this.ms1 = Arrays.stream(ms1Files.split(",")).map(File::new).collect(Collectors.toList());
        }
        public List<File> ms1;

        @CommandLine.Option(names = {"-2", "--ms2"}, description = "MS2 spectra file names", required = true, paramLabel = "<ms2File>[,<ms2File>...]", order = 132)
        protected void setMs2(String ms2Files){
            this.ms2 = Arrays.stream(ms2Files.split(",")).map(File::new).collect(Collectors.toList());
        }
        public List<File> ms2;

        @CommandLine.Option(names = {"-z", "--parentmass", "--precursor", "--mz"}, description = "the mass of the parent ion for specified ms2 spectra", required = true, order = 133)
        public Double parentMz;

        @CommandLine.Option(names = {"--ionization", "--adduct"}, description = "Specify the adduct for the corresponding Compound", defaultValue = "[M+?]+", showDefaultValue = CommandLine.Help.Visibility.ALWAYS, order = 134)
        protected void setIonType(String ionType) {
            this.ionType = PrecursorIonType.fromString(ionType);
        }

        public PrecursorIonType ionType;

        @CommandLine.Option(names = {"-f", "--formula"}, description = "Specify the neutralized formula of this compound. This will be used for tree computation and no mass decomposition will be performed.", order = 134)
        public void setFormula(String formula) {
            this.formula = MolecularFormula.parseOrThrow(formula);
        }

        public MolecularFormula formula = null;
    }
}
