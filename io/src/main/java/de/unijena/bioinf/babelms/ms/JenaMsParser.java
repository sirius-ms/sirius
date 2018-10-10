/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.ForbidRecalibration;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Timeout;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.sirius.projectspace.Index;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.Parser;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenaMsParser implements Parser<Ms2Experiment> {

    public static void main(String... args) throws IOException {

        Path p = Paths.get("/home/fleisch/Downloads/demo-data/ms/Kaempferol_openms.ms");
        GenericParser<Ms2Experiment> parser = new MsExperimentParser().getParser(p.toFile());
        Ms2Experiment experiment = parser.parseFromFile(p.toFile()).get(0);
        System.out.println(experiment.getMolecularFormula());
        Iterator<Map.Entry<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation>> iterator = experiment.forEachAnnotation();
        while (iterator.hasNext()) {
            Map.Entry<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> next = iterator.next();
            Ms2ExperimentAnnotation annotation = next.getValue();
            System.out.println(annotation);
        }
        System.out.println("end");
    }

    // todo quickn dirty hack
    BufferedReader lastReader = null;
    String lastCompundName = null;

    @Override
    public Ms2Experiment parse(BufferedReader reader, URL source) throws IOException {
        ParserInstance p = null;
        try {
            if (reader == lastReader) {
                p = new ParserInstance(source, reader);
                p.newCompound(lastCompundName);
                return p.parse();
            } else {
                p = new ParserInstance(source, reader);
                return p.parse();
            }
        } finally {
            if (p != null) {
                if (p.compoundName != null) {
                    lastReader = reader;
                }
                lastCompundName = p.compoundName;
            }
        }
    }

    private static enum SPECTRUM_TYPE {MERGED_MS1, MS1, MS2, UNKNOWN}

    private static class ParserInstance {

        private ParserInstance(URL source, BufferedReader reader) {
            this.source = source;
            this.reader = reader;
            lineNumber = 0;
            this.currentSpectrum = new SimpleMutableSpectrum();
        }

        private URL source;
        private final BufferedReader reader;
        private int lineNumber;
        private String compoundName = null;
        private MolecularFormula formula;
        private int charge = 0;
        private SPECTRUM_TYPE spectrumType = SPECTRUM_TYPE.UNKNOWN;
        private PrecursorIonType ionization;
        private CollisionEnergy currentEnergy;
        private double tic = 0, parentMass = 0, retentionTime = 0, retentionTimeStart = Double.NaN, retentionTimeEnd = Double.NaN;
        private SimpleMutableSpectrum currentSpectrum;
        private ArrayList<MutableMs2Spectrum> ms2spectra = new ArrayList<MutableMs2Spectrum>();
        private ArrayList<SimpleSpectrum> ms1spectra = new ArrayList<SimpleSpectrum>();
        private SimpleSpectrum mergedMs1;
        private String inchi, inchikey, smiles, splash, spectrumQualityString;
        private MutableMs2Experiment experiment;
        private MsInstrumentation instrumentation = MsInstrumentation.Unknown;
        private AdditionalFields fields;
        private Index index;
        private HashMap<Class<? extends Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> annotations;
        private double treeTimeout;
        private double compoundTimeout;
        private double ppmMax = 0d, ppmMaxMs2 = 0d, noiseMs2 = 0d;

        private TIntObjectMap<AdditionalFields> additionalFields = new TIntObjectHashMap<>();

        private void newCompound(String name) {
            inchi = null;
            inchikey = null;
            smiles = null;
            splash = null;
            ms1spectra = new ArrayList<>();
            mergedMs1 = null;
            ms2spectra = new ArrayList<>();
            tic = 0;
            parentMass = 0;
            retentionTime = 0;
            currentEnergy = null;
            ionization = null;
            spectrumType = SPECTRUM_TYPE.UNKNOWN;
            charge = 0;
            formula = null;
            compoundName = name;
            instrumentation = MsInstrumentation.Unknown;
            annotations = new HashMap<>();
            treeTimeout = 0d;
            compoundTimeout = 0d;
            ppmMax = ppmMaxMs2 = noiseMs2 = 0d;
        }

        private MutableMs2Experiment parse() throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    ++lineNumber;
                    if (line.isEmpty()) {
                        parseEmptyLine();
                    } else {
                        final char firstCharacter = line.charAt(0);
                        if (firstCharacter == '>') {
                            if (parseOption(line))
                                return experiment;
                        } else if (firstCharacter == '#') {
                            parseComment(line);
                        } else if (Character.isDigit(firstCharacter)) {
                            parsePeak(line);
                        } else {
                            final Matcher m = LINE_PATTERN.matcher(line);
                            if (m.find()) {
                                final char token = m.group(1).charAt(0);
                                switch (token) {
                                    case '>':
                                        parseOption(line.trim());
                                        break;
                                    case '#':
                                        parseComment(line.trim());
                                        break;
                                    default:
                                        parsePeak(line.trim());
                                }
                            } else if (line.trim().isEmpty()) {
                                parseEmptyLine();
                            } else {
                                throw new IOException("Cannot parse line " + lineNumber + ":'" + line + "'");
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    error(e.toString());
                }
            }
            flushCompound();
            return experiment;
        }

        private static Pattern LINE_PATTERN = Pattern.compile("^\\s*([>#]|\\d)");

        private static final String decimalPattern = "[+-]?\\s*\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?";

        private static final Pattern MASS_PATTERN = Pattern.compile("(" + decimalPattern + ")(?:\\s*Da)?");

        private static final Pattern FLOAT_PATTERN = Pattern.compile("(" + decimalPattern + ")");

        private static final Pattern COLLISION_PATTERN = Pattern.compile("((" + decimalPattern + ")?|((?:Ramp\\s*)?" + decimalPattern + "\\s*-\\s*" + decimalPattern + "))");

        private static final Pattern RETENTION_PATTER = Pattern.compile("(?:PT)?(" + decimalPattern + ")S?");

        private static final Pattern PEAK_PATTERN = Pattern.compile("(" + decimalPattern + ")\\s+(" + decimalPattern + ")");

        private static final Pattern TIME_PATTERN = Pattern.compile("(" + decimalPattern + ")\\s*[sS]?");

        private static final Pattern ION_WITH_OR_WIHOUT_PROB_PATTERN = Pattern.compile("\\s*([^\\(\\)]*)(\\s*\\((" + decimalPattern + ")\\))?"); //ion not clearly specified

        private boolean parseOption(String line) throws IOException {
            final String[] options = line.substring(line.indexOf('>') + 1).split("\\s+", 2);
            final String optionName = options[0].toLowerCase();
            final String value = options.length == 2 ? options[1] : "";
            if (optionName.equals("compound")) {
                final boolean newCompound = compoundName != null;
                if (newCompound) {
                    flushCompound();
                }
                newCompound(value);
                if (newCompound) return true;
            } else if (optionName.startsWith("instrument")) {
                final String v = value.toLowerCase();
                for (MsInstrumentation.Instrument i : MsInstrumentation.Instrument.values()) {
                    if (i.isInstrument(v)) {
                        instrumentation = i;
                        break;
                    }
                }
            } else if (optionName.equals("source")) {
                //override in source set in ms file
                this.source = new URL(value);
            } else if (optionName.equals("index")) {
                this.index = new Index(Integer.parseInt(value));
            } else if (optionName.equals("formula")) {
                if (formula != null || annotations.containsKey(Whiteset.class)) warn("Molecular formula is set twice");
                MolecularFormula[] formulas = parseFormulas(value);
                if (formulas.length == 1) this.formula = formulas[0];
                else {
                    Whiteset whiteset = Whiteset.of(formulas);
                    annotations.put(Whiteset.class, whiteset);
                }
            } else if (optionName.equals("parentmass")) {
                if (parentMass != 0) warn("parent mass is set twice");
                final Matcher m = MASS_PATTERN.matcher(value);
                if (m.find())
                    this.parentMass = Double.parseDouble(m.group(1));
                else
                    error("Cannot parse parent mass: '" + value + "'");
            } else if (optionName.equals("charge")) {
                final Matcher m = FLOAT_PATTERN.matcher(value);
                if (m.find()) {
                    this.charge = (int) Double.parseDouble(m.group(1));
                } else {
                    error("Cannot parse charge '" + value + "'");
                }
            } else if (optionName.equalsIgnoreCase("inchi")) {
                if (value.startsWith("InChI=")) {
                    inchi = value.trim();
                }
            } else if (optionName.equalsIgnoreCase("inchikey")) {
                inchikey = value.trim();
            } else if (optionName.equalsIgnoreCase("smarts") || optionName.equalsIgnoreCase("smiles")) {
                smiles = value;
            } else if (optionName.equalsIgnoreCase("splash")) {
                splash = value;
            } else if (optionName.equalsIgnoreCase("quality")) {
                spectrumQualityString = value;
            } else if (optionName.contains("collision") || optionName.contains("energy") || optionName.contains("ms2")) {
                if (currentSpectrum.size() > 0) newSpectrum();
                this.spectrumType = SPECTRUM_TYPE.MS2;
                if (currentEnergy != null) warn("Collision energy is set twice");
                if (value.isEmpty()) this.currentEnergy = CollisionEnergy.none();
                else {
                    final Matcher m = COLLISION_PATTERN.matcher(value);
                    if (m.find()) {
                        if (m.group(1) != null) {
                            final double val = Double.parseDouble(m.group(1));
                            currentEnergy = new CollisionEnergy(val, val);
                        } else {
                            assert m.group(2) != null;
                            final String[] range = m.group(2).split("\\s*-\\s*", 2);
                            this.currentEnergy = new CollisionEnergy(Double.parseDouble(range[0]), Double.parseDouble(range[1]));
                        }
                    } else {
                        error("Cannot parse collision '" + value + "'");
                    }
                }
            } else if (optionName.equals("tic")) {
                if (currentSpectrum.size() > 0) newSpectrum();
                if (tic != 0) warn("total ion count is set twice");
                final Matcher m = FLOAT_PATTERN.matcher(value);
                if (m.find()) {
                    tic = Double.parseDouble(value);
                } else {
                    error("Cannot parse total ion count: '" + value + "'");
                }
            } else if (optionName.contains("ms1merged")) {
                if (currentSpectrum.size() > 0) newSpectrum();
                this.spectrumType = SPECTRUM_TYPE.MERGED_MS1;
            } else if (optionName.contains("ms1")) {
                if (currentSpectrum.size() > 0) newSpectrum();
                this.spectrumType = SPECTRUM_TYPE.MS1;
            } else if (optionName.equalsIgnoreCase("rt") || optionName.equalsIgnoreCase("retention")) {
                retentionTime = parseRetentionTimeMiddle(value);
            } else if (optionName.equalsIgnoreCase("rt_start")) {
                retentionTimeStart = parseRetentionTime(value);
            } else if (optionName.equalsIgnoreCase("rt_end")) {
                retentionTimeEnd = parseRetentionTime(value);
            } else if (optionName.contains("ion") || optionName.equalsIgnoreCase("adduct")) {
                parseIonizations(value);
            } else if (optionName.equals("elements")) {
//                annotations.put(FormulaConstraints.class, new FormulaConstraints(value));
            } else if (optionName.equals("ppm-max")) {
                ppmMax = Double.parseDouble(value);
            } else if (optionName.equals("ppm-max-ms2")) {
                ppmMaxMs2 = Double.parseDouble(value);
            } else if (optionName.equals("noise")) {
                noiseMs2 = Double.parseDouble(value);
            } else if (optionName.equals("profile")) {
                warn("option '>profile' is currently not parsed from .ms file"); //TODO include somehow
            } else if (optionName.equals("compound-timeout")) {
                double t = parseTime(value);
                if (Double.isNaN(t)) warn("Cannot parse compound-timeout.");
                else compoundTimeout = t;
            } else if (optionName.equals("tree-timeout")) {
                double t = parseTime(value);
                if (Double.isNaN(t)) warn("Cannot parse tree-timeout.");
                else treeTimeout = t;
            } else if (optionName.equals("no-recalibration")) {
                experiment.setAnnotation(ForbidRecalibration.class, ForbidRecalibration.FORBIDDEN);
            } else {
                warn("Unknown option " + "'>" + optionName + "'" + " in .ms file. Option will be ignored");
                if (fields == null) fields = new AdditionalFields();
                fields.put(optionName, value);
            }
            return false;
        }

        private void flushCompound() {
            newSpectrum();
            postprocess();
            experiment = null;
            if (compoundName == null) return;
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setIonMass(parentMass);
            exp.setMolecularFormula(formula);
            exp.setName(compoundName);
            if (ionization == null) {
                if (charge != 0) {
                    exp.setPrecursorIonType(PrecursorIonType.unknown(charge));
                }
            } else {
                exp.setPrecursorIonType(ionization);
            }
            exp.setMs1Spectra(ms1spectra);
            exp.setMs2Spectra(ms2spectra);
            if (mergedMs1 != null) exp.setMergedMs1Spectrum(mergedMs1);
            exp.setSource(source);
            if (index != null) exp.setAnnotation(Index.class, index);
            if (smiles != null) exp.setAnnotation(Smiles.class, new Smiles(smiles));
            if (splash != null) exp.setAnnotation(Splash.class, new Splash(splash));
            if (spectrumQualityString != null)
                exp.setAnnotation(CompoundQuality.class, CompoundQuality.fromString(spectrumQualityString));
            if (inchi != null || inchikey != null) exp.setAnnotation(InChI.class, new InChI(inchikey, inchi));
            if (instrumentation != null) exp.setAnnotation(MsInstrumentation.class, instrumentation);
            if (retentionTime != 0)
                exp.setAnnotation(RetentionTime.class, new RetentionTime(retentionTimeStart, retentionTimeEnd, retentionTime));
            if (fields != null) exp.setAnnotation(AdditionalFields.class, fields);
            for (Map.Entry<Class<? extends Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> entry : annotations.entrySet()) {
                exp.setAnnotation((Class<Ms2ExperimentAnnotation>) entry.getKey(), entry.getValue());
            }
            if (compoundTimeout != 0 || treeTimeout != 0) {
                Timeout timeout = Timeout.newTimeout((int) compoundTimeout, (int) treeTimeout);
                exp.setAnnotation(Timeout.class, timeout);
            }
            this.experiment = exp;
            fields = null;
            this.compoundName = null;
        }

        private void error(String s) throws IOException {
            throw new IOException(lineNumber + ": " + s);
        }

        private void warn(String msg) {
            LoggerFactory.getLogger(this.getClass()).warn(lineNumber + ": " + msg);
        }

        private void parseComment(String line) {
            int depth = 0;
            while (line.charAt(depth) == '#') depth++;

            final String[] options = line.substring(depth).split("\\s+", 2);
            final String optionName = options[0].toLowerCase();
            final String value = options.length == 2 ? options[1] : "";


            if (depth == 1) {
                if (fields == null) fields = new AdditionalFields();
                fields.put(optionName, value);
            } else if (depth == 2) {
                if (currentSpectrum != null)
                    currentSpectrum.computeAnnotationIfAbsent(AdditionalFields.class, (it) -> new AdditionalFields()).put(optionName, value);
                else
                    warn("Cannot parse spectrum level (##) comment (" + line + ") because there is no Spectrum it belongs to.");
            }
        }

        private double parseRetentionTimeMiddle(String value) {
            if (currentSpectrum.size() > 0) newSpectrum();
            final double p = parseRetentionTime(value);
            return Double.isNaN(p) ? 0 : p;
        }

        private double parseRetentionTime(String value) {
            final Matcher m = RETENTION_PATTER.matcher(value);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            } else {
                warn("Cannot parse retention time: '" + value + "'");
                return Double.NaN;
            }
        }

        private double parseTime(String value) {
            final Matcher m = TIME_PATTERN.matcher(value);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            } else {
                return Double.NaN;
            }
        }

        private void parsePeak(String line) throws IOException {
            final Matcher m = PEAK_PATTERN.matcher(line);
            if (m.find()) {
                currentSpectrum.addPeak(new Peak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))));
            } else {
                error("Cannot parse peak '" + line + "'");
            }
        }

        //todo ms1 annotations lost through copy cascade
        private void newSpectrum() {
            if (currentSpectrum.size() > 0) {
                if (spectrumType == SPECTRUM_TYPE.MERGED_MS1) {
                    mergedMs1 = new SimpleSpectrum(currentSpectrum);
                } else if (spectrumType == SPECTRUM_TYPE.MS1) {
                    ms1spectra.add(new SimpleSpectrum(currentSpectrum));
                } else if (spectrumType == SPECTRUM_TYPE.MS2) {
                    ms2spectra.add(new MutableMs2Spectrum(currentSpectrum, parentMass, currentEnergy, 2));
                } else {
                    warn("Unknown spectrum type. Description must contain one of the following keywords '>[ms1|mergedms1|ms2|collision|energy]'. " +
                            "Spectrum will be processed as MS2 spectrum.");
                    ms2spectra.add(new MutableMs2Spectrum(currentSpectrum, parentMass, currentEnergy, 2));
                }
            } else return;
            spectrumType = SPECTRUM_TYPE.UNKNOWN;
            this.tic = 0;
//            this.retentionTime = 0d; //todo currently the retention time is a MsExperiment attribute, not a spectrum property
            this.currentEnergy = null;
            this.currentSpectrum = new SimpleMutableSpectrum();
        }

        private void parseEmptyLine() {
            newSpectrum();
        }


        private MolecularFormula[] parseFormulas(String formulas) {
            if (formulas.contains(",")) {
                return Arrays.stream(formulas.split(",")).map(s -> MolecularFormula.parse(s)).toArray(MolecularFormula[]::new);
            } else {
                return new MolecularFormula[]{MolecularFormula.parse(formulas)};
            }
        }

        private void parseIonizations(String ions) {
            if (ions.contains(",")) {
                //todo support ionizations with modifications as PossibleAdducts?
                String[] arr = ions.split(",");
                PrecursorIonType[] ionTypes = new PrecursorIonType[arr.length];
                double[] probabilities = new double[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    String s = arr[i];
                    final Matcher m = ION_WITH_OR_WIHOUT_PROB_PATTERN.matcher(s);
                    if (m.find()) {
                        final PrecursorIonType ion = PeriodicTable.getInstance().ionByName(m.group(1).trim());
                        if (ion == null) {
                            warn("Unknown ionization in: '" + s + "'");
                            return;
                        }
                        if (!ion.hasNeitherAdductNorInsource()) {
                            warn("Currently only simple ionization (e.g. [M+Na]+) without additional modifications or insource fragments are supported.");
                        }
                        ionTypes[i] = ion;
                        if (m.group(3) != null && m.group(3).length() > 0) {
                            probabilities[i] = Double.parseDouble(m.group(3));
                        } else probabilities[i] = 1d;
                    } else {
                        warn("Cannot parse ionizations: '" + ions + "'");
                        return;
                    }
                }
                PossibleIonModes ionModes = new PossibleIonModes();
                for (int i = 0; i < ionTypes.length; i++) {
                    PrecursorIonType ionType = ionTypes[i];
                    double p = probabilities[i];
                    ionModes.add(ionType, p);
                }
                annotations.put(PossibleIonModes.class, ionModes);
            } else {
                final PrecursorIonType ion = PeriodicTable.getInstance().ionByName(ions.trim());
                if (ion == null) {
                    warn("Unknown ionization: '" + ions + "'");
                    return;
                }
                this.ionization = ion;
            }
        }

        private Ms2MutableMeasurementProfileDummy getMs2Profile() {
            Ms2MutableMeasurementProfileDummy ms2Profile = (Ms2MutableMeasurementProfileDummy) annotations.get(Ms2MutableMeasurementProfileDummy.class);
            if (ms2Profile == null) {
                ms2Profile = new Ms2MutableMeasurementProfileDummy();
                annotations.put(Ms2MutableMeasurementProfileDummy.class, ms2Profile);
            }
            return ms2Profile;
        }

        private Ms1MutableMeasurementProfileDummy getMs1Profile() {
            Ms1MutableMeasurementProfileDummy ms1Profile = (Ms1MutableMeasurementProfileDummy) annotations.get(Ms1MutableMeasurementProfileDummy.class);
            if (ms1Profile == null) {
                ms1Profile = new Ms1MutableMeasurementProfileDummy();
                annotations.put(Ms1MutableMeasurementProfileDummy.class, ms1Profile);
            }
            return ms1Profile;
        }

        private void postprocess() {
            setMeasurementProfiles();
        }

        private void setMeasurementProfiles() {
            if (ppmMax == 0 && ppmMaxMs2 == 0 && noiseMs2 == 0) return;
            if (ppmMaxMs2 == 0) ppmMaxMs2 = ppmMax;
            if (ppmMax != 0) {
                Ms1MutableMeasurementProfileDummy ms1Profile = getMs1Profile();
                ms1Profile.setAllowedMassDeviation(new Deviation(ppmMax));
            }
            Ms2MutableMeasurementProfileDummy ms2Profile = null;
            if (ppmMaxMs2 != 0) {
                ms2Profile = getMs2Profile();
                ms2Profile.setAllowedMassDeviation(new Deviation(ppmMaxMs2));
            }
            if (noiseMs2 != 0) {
                ms2Profile = getMs2Profile();
                ms2Profile.setMedianNoiseIntensity(noiseMs2);
            }
        }

    }
}
