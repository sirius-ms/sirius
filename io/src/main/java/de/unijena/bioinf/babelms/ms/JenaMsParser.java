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
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.ForbidRecalibration;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Timeout;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
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

//        Path p = Paths.get("/home/fleisch/Downloads/demo-data/ms/Kaempferol_openms.ms");
        Path p = Paths.get("/home/fleisch/Downloads/demo/demo-data/ms/Kaempferol.ms");
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
            this.source = new MsFileSource(source);
            this.reader = reader;
            lineNumber = 0;
            this.currentSpectrum = new SimpleMutableSpectrum();
        }

        private Set<Class<Ms2ExperimentAnnotation>> configKeys;
        private DefaultParameterConfig config;

        private final MsFileSource source;
        private SpectrumFileSource externalSource;
        private final BufferedReader reader;
        private int lineNumber;
        private String compoundName = null;
        private Whiteset formulas;

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
//        private double treeTimeout;
//        private double compoundTimeout;
//        private double ppmMax = 0d, ppmMaxMs2 = 0d, noiseMs2 = 0d;

        //these are comments/additional options or metainfos that are mot nessecarily used by sirius
        private AdditionalFields fields;



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
            formulas = null;
            compoundName = name;
            instrumentation = MsInstrumentation.Unknown;
//            annotations = new HashMap<>();
//            treeTimeout = 0d;
//            compoundTimeout = 0d;
//            ppmMax = ppmMaxMs2 = noiseMs2 = 0d;
            config = PropertyManager.DEFAULTS.newIndependendInstance();
            configKeys = new LinkedHashSet<>();

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
            } else if (config.containsConfigKey(optionName)) {
                Class<?> cls = config.changeConfig(optionName, value);
                if (Ms2ExperimentAnnotation.class.isAssignableFrom(cls))
                    configKeys.add((Class<Ms2ExperimentAnnotation>) cls);
            } else if (optionName.equals("index")) {
                //set additional index for backward compatibility
                //index is replaced against .index file during new
                //project-space implementation. This is for compatibility
                final Integer index = Integer.parseInt(value);
                addAsAdditionalField("index", index.toString());
            } else if (optionName.equals("source")) {
                //override in source set in ms file
                this.externalSource = new SpectrumFileSource(new URL(value));
            } else if (optionName.equals("formulas")) {
                if (formulas == null) {
                    formulas = Whiteset.of(parseFormulas(value));
                } else {
                    warn("Molecular formulas is set twice, Sirius will collect them as a Whitelist!");
                    formulas.getFormulas().addAll(Arrays.asList(parseFormulas(value)));
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
                changeConfig("FormulaSettings.detectable", value);
                changeConfig("FormulaSettings.fallback", value);
            } else if (optionName.equals("ppm-max")) {
                changeConfig("MS1MassDeviation.allowedMassDeviation", value);
            } else if (optionName.equals("ppm-max-ms2")) {
                changeConfig("MS2MassDeviation.allowedMassDeviation", value);
            } else if (optionName.equals("noise")) {
                changeConfig("MedianNoiseIntensity", value);
            } else if (optionName.equals("compound-timeout")) {
                changeConfig("Timeout.secondsPerInstance", String.valueOf(parseTime(value)));
            } else if (optionName.equals("tree-timeout")) {
                changeConfig("Timeout.secondsPerTree", String.valueOf(parseTime(value)));
            } else if (optionName.equals("no-recalibration")) {
                changeConfig("ForbidRecalibration", ForbidRecalibration.FORBIDDEN.name());
            } else {
                warn("Unknown option " + "'>" + optionName + "'" + " in .ms file. Option will be ignored");
                if (fields == null) fields = new AdditionalFields();
                fields.put(optionName, value);
            }
            return false;
        }

        private void addAsAdditionalField(@NotNull String key, @NotNull String value) {
            if (fields == null) fields = new AdditionalFields();
            fields.put(key, value);
        }

        private void changeConfig(@NotNull String key, @NotNull String value) throws IOException {
            try {
                configKeys.add((Class<Ms2ExperimentAnnotation>) config.changeConfig(key, value));
            } catch (Throwable e) {
                error("Could not parse Config key: " + key);
            }
        }

        private void flushCompound() {
            newSpectrum();
            experiment = null;
            if (compoundName == null) return;
            final MutableMs2Experiment exp = new MutableMs2Experiment();

            //set fields
            exp.setIonMass(parentMass);
            if (formulas != null) {
                if (formulas.getFormulas().size() == 1)
                    exp.setMolecularFormula(formulas.getFormulas().iterator().next());
                else
                    exp.setAnnotation(Whiteset.class, formulas);
            }
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
            exp.setAnnotation(MsFileSource.class, source);
            if (externalSource != null)
                exp.setAnnotation(SpectrumFileSource.class, externalSource);

            if (smiles != null) exp.setAnnotation(Smiles.class, new Smiles(smiles));
            if (splash != null) exp.setAnnotation(Splash.class, new Splash(splash));

            if (spectrumQualityString != null)
                exp.setAnnotation(CompoundQuality.class, CompoundQuality.fromString(spectrumQualityString));
            if (inchi != null || inchikey != null) exp.setAnnotation(InChI.class, new InChI(inchikey, inchi));
            if (instrumentation != null) exp.setAnnotation(MsInstrumentation.class, instrumentation);
            if (retentionTime != 0)
                exp.setAnnotation(RetentionTime.class, new RetentionTime(retentionTimeStart, retentionTimeEnd, retentionTime));

            //add config annotations that have been set within the file
            configKeys.forEach(cls -> exp.setAnnotation(cls, config.createInstanceWithDefaults(cls)));

            //add additional fields
            if (fields != null) exp.setAnnotation(AdditionalFields.class, fields);

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
                    currentSpectrum.computeAnnotationIfAbsent(AdditionalFields.class).put(optionName, value);
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
            //always parse empty MS1/MS2. else this might create issues with MS1/MS2 mapping
            if (spectrumType == SPECTRUM_TYPE.MS1) {
                ms1spectra.add(new SimpleSpectrum(currentSpectrum));
            } else if (spectrumType == SPECTRUM_TYPE.MS2) {
                ms2spectra.add(new MutableMs2Spectrum(currentSpectrum, parentMass, currentEnergy, 2));
            } else if (currentSpectrum.size() > 0) {
                if (spectrumType == SPECTRUM_TYPE.MERGED_MS1) {
                    mergedMs1 = new SimpleSpectrum(currentSpectrum);
                } else {
                    warn("Unknown spectrum type. Description must contain one of the following keywords '>[ms1|mergedms1|ms2|collision|energy]'. " +
                            "Spectrum will be processed as MS2 spectrum.");
                    ms2spectra.add(new MutableMs2Spectrum(currentSpectrum, parentMass, currentEnergy, 2));
                }
            } else return;
            spectrumType = SPECTRUM_TYPE.UNKNOWN;
            this.tic = 0;
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

        private void parseIonizations(String ions) throws IOException {
            if (ions.contains(",")) {
                changeConfig("AdductSettings.enforced",ions);
                /*String[] arr = ions.split(",");
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
                final Set<PrecursorIonType> ionTypeSet = new HashSet<>();
                for (int i = 0; i < ionTypes.length; i++) {
                    PrecursorIonType ionType = ionTypes[i];
                    ionTypeSet.add(ionType);
                    if (probabilities[i]>1) {
                        warn("Probabilities for ion types are currently not supported");
                    }
                }
                final AdductSettings s = this.experiment.getAnnotationOrDefault(AdductSettings.class);
                annotations.put(AdductSettings.class, s.withEnforced(ionTypeSet));*/
            } else {
                final PrecursorIonType ion = PeriodicTable.getInstance().ionByName(ions.trim());
                if (ion == null) {
                    warn("Unknown ionization: '" + ions + "'");
                    return;
                }
                this.ionization = ion;
            }
        }


        /*private void postprocess() {
            setMassDeviations();
        }*/

        /*private void setMassDeviations() {
            if (ppmMax == 0 && ppmMaxMs2 == 0 && noiseMs2 == 0) return;
            if (ppmMaxMs2 == 0) ppmMaxMs2 = ppmMax;
            if (ppmMax != 0) {
                annotations.put(MS1MassDeviation.class, PropertyManager.DEFAULTS.createInstanceWithDefaults(MS1MassDeviation.class).withAllowedMassDeviation(new Deviation(ppmMax)));
            }

            if (ppmMaxMs2 != 0) {
                annotations.put(MS2MassDeviation.class, PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).withAllowedMassDeviation(new Deviation(ppmMaxMs2)));
            }

            if (noiseMs2 != 0) {
                annotations.put(MedianNoiseIntensity.class, new MedianNoiseIntensity(noiseMs2));
            }
        }*/

    }
}
