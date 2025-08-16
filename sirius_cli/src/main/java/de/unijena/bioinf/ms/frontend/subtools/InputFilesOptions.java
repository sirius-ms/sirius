/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.InstanceImporter;
import lombok.Getter;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputFilesOptions {

    public InputFilesOptions() {
    }

    public Stream<Path> getAllFilesStream() {
        if (msInput == null)
            return Stream.of();
        return Stream.of(msInput.msParserfiles.keySet(), msInput.unknownFiles.keySet()).flatMap(Collection::stream);
    }

    public List<Path> getAllFiles() {
        return getAllFilesStream().collect(Collectors.toList());
    }

    public File[] getAllFilesArray() {
        return getAllFilesStream().map(Path::toFile).toArray(File[]::new);
    }

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold Specify multi-compound inputs (.ms, .mgf, .mzML/.mzXml):%n|@", order = 320)
    public MsInput msInput;


    public static class MsInput {
        public final Map<Path, Integer> lcmsFiles, msParserfiles, unknownFiles;

        public MsInput() {
            this(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        public MsInput(Map<Path, Integer> lcmsFiles, Map<Path, Integer> msParserfiles, Map<Path, Integer> unknownFiles) {
            this.lcmsFiles = lcmsFiles;
            this.msParserfiles = msParserfiles;
            this.unknownFiles = unknownFiles;
        }

        @Getter
        protected List<Path> rawInputFiles;

        //todo comma separation is deprecated and should be removed in next major update.
        @CommandLine.Option(names = {"--input", "-i"}, arity = "1..*", description = "Specify the input in multi-compound input formats: Preprocessed mass spectra in .ms or .mgf file format or " +
                "LC/MS runs in .mzML/.mzXml format but also any other file type e.g. to provide input for STANDALONE tools.", required = true, split = ",", order = 321)
        public void setInputPath(List<Path> files) {
            lcmsFiles.clear();
            msParserfiles.clear();
            unknownFiles.clear();
            rawInputFiles = files;
            InstanceImporter.expandInput(files, this);
        }

        @CommandLine.Option(names = {"--ignore-formula"}, description = "ignore given molecular formula if present in .ms or .mgf input files.", defaultValue = "false", order = 322)
        public void setIgnoreFormula(boolean ignoreFormula) {
            this.ignoreFormula = ignoreFormula;
        }

        @Getter
        private boolean ignoreFormula;

        @Deprecated(forRemoval = true)
        @CommandLine.Option(names = {"--allow-ms1-only"}, description = "Allow MS1 only data to be imported.", defaultValue = "false", order = 323, hidden = true)
        public void setAllowMS1Only(boolean allowMS1Only) {
            this.allowMS1Only = allowMS1Only;
        }

        @Getter
        private boolean allowMS1Only;

        public boolean isEmpty() {
            return msParserfiles.isEmpty() && lcmsFiles.isEmpty() && unknownFiles.isEmpty();
        }
    }

    // region Options: CSV Input
    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "@|bold Specify generic inputs (CSV) on per compound level:%n|@", order = 330)
    public List<CsvInput> csvInputs;

    public static class CsvInput {
        @CommandLine.Option(names = {"-1", "--ms1"}, description = "MS1 spectra files", paramLabel = "<ms1File>[,<ms1File>...]", order = 331)
        protected void setMs1(String ms1Files) {
            this.ms1 = Arrays.stream(ms1Files.split(",")).map(File::new).collect(Collectors.toList());
        }

        public List<File> ms1;

        @CommandLine.Option(names = {"-2", "--ms2"}, description = "MS2 spectra files", required = true, paramLabel = "<ms2File>[,<ms2File>...]", order = 332)
        protected void setMs2(String ms2Files) {
            this.ms2 = Arrays.stream(ms2Files.split(",")).map(File::new).collect(Collectors.toList());
        }

        public List<File> ms2;

        @CommandLine.Option(names = {"-z", "--parentmass", "--precursor", "--mz"}, description = "The mass of the parent ion for the specified ms2 spectra", required = true, order = 333)
        public Double parentMz;

        @CommandLine.Option(names = {"--ionization", "--adduct"}, description = "Specify the adduct for this compound", defaultValue = "[M+?]+", order = 334)
        protected void setIonType(String ionType) {
            this.ionType = PrecursorIonType.fromString(ionType);
        }

        public PrecursorIonType ionType;

        @CommandLine.Option(names = {"-f", "--formula"}, description = "Specify the neutralized formula of this compound. This will be used for tree computation. If given no mass decomposition will be performed.", order = 335)
        public void setFormula(String formula) {
            this.formula = MolecularFormula.parseOrThrow(formula);
        }

        public MolecularFormula formula = null;
    }
}
