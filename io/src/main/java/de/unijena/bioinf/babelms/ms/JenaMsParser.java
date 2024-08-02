
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.Tagging;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.CandidateFormulas;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.ForbidRecalibration;
import de.unijena.bioinf.ChemistryBase.ms.utils.*;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.utils.ParserUtils;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the .ms file format. This parser does not set all default parameters to the {@link Ms2Experiment}. It only annotates parameters directly set in the file.
 * {@link MsExperimentParser} also annotates default parameters to an experiment (what is generally desired).
 */
@Slf4j
public class JenaMsParser implements Parser<Ms2Experiment> {

    public static void main(String... args) throws IOException {

//        Path p = Paths.get("/home/fleisch/Downloads/demo-data/ms/Kaempferol_openms.ms");
        Path p = Paths.get("/home/fleisch/Downloads/demo/demo-data/ms/Kaempferol.ms");
        GenericParser<Ms2Experiment> parser = new MsExperimentParser().getParser(p.toFile());
        Ms2Experiment experiment = parser.parseFromFile(p.toFile()).get(0);
        System.out.println(experiment.getMolecularFormula());
        Iterator<Map.Entry<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation>> iterator = experiment.annotationIterator();
        while (iterator.hasNext()) {
            Map.Entry<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> next = iterator.next();
            Ms2ExperimentAnnotation annotation = next.getValue();
            System.out.println(annotation);
        }
        System.out.println("end");
    }

    Object lastSource = null;
    String lastCompoundName = null;
    BufferedReader reader = null;
    @Override
    public Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        if (lastSource != inputStream || reader == null)
            reader = FileUtils.ensureBuffering(new InputStreamReader(inputStream));
        return parse(inputStream,  source, PropertyManager.DEFAULTS);
    }

    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        if (lastSource != reader || reader == null)
            this.reader = reader;
        return parse(reader, source, PropertyManager.DEFAULTS);
    }

    private Ms2Experiment parse(Object currentSource, URI source, ParameterConfig config) throws IOException {

        ParserInstance p = null;
        while (true) {
            try {
                p = new ParserInstance(source, reader, config);
                if (currentSource == lastSource) {
                    p.newCompound(lastCompoundName);
                }
                return p.parse();
            } catch (IOException e) {
                log.warn("Error when parsing Compound '" + p.compoundName + "'. Skipping this entry!", e);
            } finally {
                if (p != null) {
                    if (p.compoundName != null) {
                        lastSource = currentSource;
                    }
                    lastCompoundName = p.compoundName;
                }
            }
        }
    }

    private enum SPECTRUM_TYPE {MERGED_MS1, MS1, MS2, UNKNOWN}

    private static class ParserInstance {

        private ParserInstance(URI source, BufferedReader reader, ParameterConfig baseConfig) {
            this.source = new MsFileSource(source);
            this.reader = reader;
            lineNumber = 0;
            this.currentSpectrum = AnnotatedWrapperSpectrum.of(new SimpleMutableSpectrum());
            this.baseConfig = baseConfig;
        }

        private final ParameterConfig baseConfig;
        private ParameterConfig config;

        private final MsFileSource source;
        private SpectrumFileSource externalSource;
        private final BufferedReader reader;
        private int lineNumber;
        private String compoundName = null;
        private CandidateFormulas formulas;
        private List<String> tags;

        //private NoiseInformation noiseInformation;

        private List<List<String>> ms1Comments, ms2Comments;
        private boolean hasPeakComment;
        private List<String> mergedComments;
        private List<String> currentComments;

        private int charge = 0;
        private SPECTRUM_TYPE spectrumType = SPECTRUM_TYPE.UNKNOWN;
        private PrecursorIonType ionization;

        private Quantification quant;

        private CollisionEnergy currentEnergy;
        private double tic = 0, parentMass = 0, retentionTime = 0, retentionTimeStart = Double.NaN, retentionTimeEnd = Double.NaN;
        private double retentionTimeMin, retentionTimeMax;
        private AnnotatedWrapperSpectrum<SimpleMutableSpectrum, Peak> currentSpectrum;
        private ArrayList<MutableMs2Spectrum> ms2spectra = new ArrayList<>();
        private ArrayList<SimpleSpectrum> ms1spectra = new ArrayList<>();
        private SimpleSpectrumWithAdditionalFields mergedMs1;
        private String inchi, inchikey, smiles, splash, spectrumQualityString, featureId;
        private MutableMs2Experiment experiment;
        private MsInstrumentation instrumentation = MsInstrumentation.Unknown;

        //these are comments/additional options or metainfos that are mot nessecarily used by sirius
        private AdditionalFields fields;



        private void newCompound(String name) {
            //noiseInformation = null;
            inchi = null;
            inchikey = null;
            smiles = null;
            splash = null;
            featureId = null;
            ms1spectra = new ArrayList<>();
            mergedMs1 = null;
            ms2spectra = new ArrayList<>();
            tic = 0;
            parentMass = 0;
            retentionTime = 0;
            retentionTimeMin = Double.MAX_VALUE;
            retentionTimeMax = Double.MIN_VALUE;
            currentEnergy = null;
            ionization = null;
            spectrumType = SPECTRUM_TYPE.UNKNOWN;
            charge = 0;
            quant = null;
            formulas = null;
            compoundName = name;
            this.tags = new ArrayList<>();
            ms1Comments = new ArrayList<>();
            ms2Comments = new ArrayList<>();
            mergedComments = new ArrayList<>();
            currentComments = new ArrayList<>();
            hasPeakComment = false;
            instrumentation = MsInstrumentation.Unknown;
//            annotations = new HashMap<>();
//            treeTimeout = 0d;
//            compoundTimeout = 0d;
//            ppmMax = ppmMaxMs2 = noiseMs2 = 0d;
            config = baseConfig.newIndependentInstance("MS_FILE:" + name);
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
                            parsePeak(line, currentComments);
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
                                        parsePeak(line.trim(),currentComments);
                                }
                            } else if (line.trim().isEmpty()) {
                                parseEmptyLine();
                            } else {
                                throw new IOException("Cannot parse line " + lineNumber + ":'" + line + "'");
                            }
                        }
                    }
                } catch (IOException | RuntimeException e) {
//                   go to next compound
                    line = reader.readLine();
                    while (line != null && !line.startsWith(">compound")) {
                        try {
                            line = reader.readLine();
                        } catch (IOException ex) {
                            log.warn("Error when cleaning up after Exception", ex);
                        }
                    }

                    if (e instanceof RuntimeException)
                        error(e.toString());
                    else throw e;
                }
            }
            flushCompound();
            return experiment;
        }

        private static final Pattern LINE_PATTERN = Pattern.compile("^\\s*([>#]|\\d)");

        private static final String decimalPattern = "[+-]?\\s*\\d+(?:[.,]\\d+)?(?:[eE][+-]?\\d+)?";

        private static final Pattern MASS_PATTERN = Pattern.compile("(" + decimalPattern + ")(?:\\s*Da)?");

        private static final Pattern FLOAT_PATTERN = Pattern.compile("(" + decimalPattern + ")");

        private static final Pattern COLLISION_PATTERN = Pattern.compile("((" + decimalPattern + ")?|((?:Ramp\\s*)?" + decimalPattern + "\\s*-\\s*" + decimalPattern + "))");

        private static final Pattern RETENTION_PATTERN = Pattern.compile("(" + decimalPattern + ")");

        private static final Pattern PEAK_PATTERN = Pattern.compile("^(" + decimalPattern + ")\\s+(" + decimalPattern + ")(?:\\s+#(.+)$)?");

        private static final Pattern TIME_PATTERN = Pattern.compile("(" + decimalPattern + ")\\s*[sS]?");

        private static final Pattern ION_WITH_OR_WIHOUT_PROB_PATTERN = Pattern.compile("\\s*([^()]*)(\\s*\\((" + decimalPattern + ")\\))?"); //ion not clearly specified

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
            } else if (optionName.equals("index")) {
                //set additional index for backward compatibility
                //index is replaced against .index file during new
                //project-space implementation. This is for compatibility
                final int index = Integer.parseInt(value);
                addAsAdditionalField("index", Integer.toString(index));
            } else if (optionName.equals("source")) {
                //override in source set in ms file
                this.externalSource = new SpectrumFileSource(URI.create(value));
            } else if (optionName.equals("formula") || optionName.equals("formulas")) {
                final List<String> valueList = Arrays.asList(value.split("\\s+|,"));
                if (this.formulas == null) {
                    this.formulas = CandidateFormulas.of(valueList, JenaMsParser.class, true, false);
                } else {
                    warn("Molecular formulas is set twice, Sirius will collect them as a Whitelist!");
                    this.formulas.addAndMerge(CandidateFormulas.of(valueList, JenaMsParser.class, true, false));
                }
            } else if (optionName.equals("parentmass")) {
                if (parentMass != 0) warn("parent mass is set twice");
                final Matcher m = MASS_PATTERN.matcher(value);
                if (m.find())
                    this.parentMass = Utils.parseDoubleWithUnknownDezSep(m.group(1));
                else
                    error("Cannot parse parent mass: '" + value + "'");
            } else if (optionName.equals("charge")) {
                final Matcher m = FLOAT_PATTERN.matcher(value);
                if (m.find()) {
                    this.charge = (int) Utils.parseDoubleWithUnknownDezSep(m.group(1));
                } else {
                    error("Cannot parse charge '" + value + "'");
                }
            } else if (optionName.equalsIgnoreCase("inchi")) {
                if (value.startsWith("InChI=")) {
                    inchi = value.trim();
                }
            } else if (optionName.toLowerCase().startsWith("tag")) {
                tags.addAll(Arrays.asList(value.split(",")));
            } else if (optionName.equalsIgnoreCase("quantification")) {
                quant = Quantification.fromString(value);
            } else if (optionName.equalsIgnoreCase("inchikey")) {
                inchikey = value.trim();
            } else if (optionName.equalsIgnoreCase("smarts") || optionName.equalsIgnoreCase("smiles")) {
                smiles = value;
            } else if (optionName.equalsIgnoreCase("splash")) {
                splash = value;
            /*
            } else if (optionName.equalsIgnoreCase("noise")) {
                noiseInformation = NoiseInformation.fromString(value);
             */
            } else if (optionName.equalsIgnoreCase("quality")) {

                spectrumQualityString = value;
            } else if (optionName.equalsIgnoreCase("rt") || optionName.equalsIgnoreCase("retention")) {
                retentionTime = parseRetentionTimeMiddle(value);
                retentionTimeMin = Math.min(retentionTimeMin, retentionTime);
                retentionTimeMax = Math.max(retentionTimeMax, retentionTime);
            } else if (optionName.equalsIgnoreCase("rt_start")) {
                retentionTimeStart = parseRetentionTime(value);
            } else if (optionName.equalsIgnoreCase("rt_end")) {
                retentionTimeEnd = parseRetentionTime(value);
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
            } else if (config.containsConfigKey(options[0])) {
                changeConfig(options[0], value);
            } else if (optionName.contains("collision") || optionName.contains("energy") || optionName.contains("ms2")) {
                if (!currentSpectrum.isEmpty()) newSpectrum();
                this.spectrumType = SPECTRUM_TYPE.MS2;
                if (currentEnergy != null) warn("Collision energy is set twice");
                if (value.isEmpty()) this.currentEnergy = CollisionEnergy.none();
                else {
                    final Matcher m = COLLISION_PATTERN.matcher(value);
                    if (m.find()) {
                        this.currentEnergy = CollisionEnergy.fromString(value);
                    } else {
                        error("Cannot parse collision '" + value + "'");
                    }
                }
            } else if (optionName.equals("tic")) {
                if (!currentSpectrum.isEmpty()) newSpectrum();
                if (tic != 0) warn("total ion count is set twice");
                final Matcher m = FLOAT_PATTERN.matcher(value);
                if (m.find()) {
                    tic = Utils.parseDoubleWithUnknownDezSep(value);
                } else {
                    error("Cannot parse total ion count: '" + value + "'");
                }
            } else if (optionName.contains("ms1merged")) {
                if (!currentSpectrum.isEmpty()) newSpectrum();
                this.spectrumType = SPECTRUM_TYPE.MERGED_MS1;
            } else if (optionName.contains("ms1")) {
                if (!currentSpectrum.isEmpty()) newSpectrum();
                this.spectrumType = SPECTRUM_TYPE.MS1;
            } else if (optionName.contains("ion") || optionName.equals("adduct")) {
                parseIonizations(value);
            } else if (optionName.equalsIgnoreCase("feature_id") || optionName.equalsIgnoreCase("feature-id") || optionName.equalsIgnoreCase("featureid")) {
                if (featureId != null) warn("feature-id has bean set set twice");
                featureId = value;
            } else {
                warn("Unknown option " + "'>" + optionName + "'" + " in .ms file. Option will be ignored, but stored as additional field.");
                if (fields == null) fields = new AdditionalFields();
                fields.put(optionName, value);
            }
            return false;
        }

        private void addAsAdditionalField(@NotNull String key, @NotNull String value) {
            if (fields == null) fields = new AdditionalFields();
            fields.put(key, value);
        }

        private Optional<Class<?>> changeConfig(@NotNull String key, @NotNull String value) throws IOException {
            try {
                return Optional.of(config.changeConfig(key, value));
            } catch (Throwable e) {
                error("Could not parse Config key = " + key + " with value = " + value + ".");
                return Optional.empty();
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
                if (formulas.numberOfFormulas() == 1)
                    exp.setMolecularFormula(formulas.getFormulas().iterator().next());
                else
                    exp.setAnnotation(CandidateFormulas.class, formulas);
            }
            exp.setName(compoundName);
            exp.setFeatureId(featureId);
            if (ionization == null) {
                if (charge != 0) {
                    exp.setPrecursorIonType(PrecursorIonType.unknown(charge));
                }
            } else {
                exp.setPrecursorIonType(ionization);
                if (!ionization.isIonizationUnknown())
                    exp.setAnnotation(DetectedAdducts.class, DetectedAdducts.singleton(DetectedAdducts.Source.INPUT_FILE, ionization));
            }
            exp.setMs1Spectra(ms1spectra);
            exp.setMs2Spectra(ms2spectra);

            exp.setAnnotation(Tagging.class, new Tagging(tags.toArray(new String[0])));

            /*
            if (noiseInformation!=null)
                exp.setAnnotation(NoiseInformation.class, noiseInformation);
            */

            if (mergedMs1 != null) exp.setMergedMs1Spectrum(mergedMs1);
            exp.setAnnotation(MsFileSource.class, source);
            if (externalSource != null)
                exp.setAnnotation(SpectrumFileSource.class, externalSource);

            if (smiles != null) exp.setAnnotation(Smiles.class, new Smiles(smiles));
            if (splash != null) exp.setAnnotation(Splash.class, new Splash(splash));

            if (spectrumQualityString != null)
                exp.setAnnotation(CompoundQuality.class, CompoundQuality.fromString(spectrumQualityString));
            if (inchi != null || inchikey != null) exp.setAnnotation(InChI.class, InChIs.newInChI(inchikey, inchi));
            if (instrumentation != null) exp.setAnnotation(MsInstrumentation.class, instrumentation);

            if (retentionTime != 0 && retentionTimeMin == retentionTimeMax) {
                exp.setAnnotation(RetentionTime.class, new RetentionTime(retentionTimeStart, retentionTimeEnd, retentionTime));
            } else if (!Double.isNaN(retentionTimeStart) && !Double.isNaN(retentionTimeEnd)) {
                exp.setAnnotation(RetentionTime.class, new RetentionTime(retentionTimeStart, retentionTimeEnd));
            } else if (retentionTimeMin < retentionTimeMax) {
                exp.setAnnotation(RetentionTime.class, new RetentionTime(retentionTimeMin, retentionTimeMax));
            }

            //add config annotations that have been set within the file
            exp.setAnnotation(InputFileConfig.class, new InputFileConfig(config)); //set map for reconstructability
            exp.setAnnotationsFrom(config.createInstancesWithModifiedDefaults(Ms2ExperimentAnnotation.class, true));

            if (quant!=null) exp.setAnnotation(Quantification.class, quant);

            //add additional fields
            if (fields != null) exp.setAnnotation(AdditionalFields.class, fields);


            if (hasPeakComment){
                String[][] ms1CommentArray = strings2arrays(ms1Comments);
                String[][] ms2CommentArray = strings2arrays(ms2Comments);
                String[] mergedComment = mergedComments.toArray(String[]::new);
                exp.setAnnotation(PeakComment.class, new PeakComment(mergedComment,ms1CommentArray,ms2CommentArray));
            }

            experiment = exp;
            fields = null;
            config = null;
            compoundName = null;

            //validate
            ParserUtils.checkMolecularFormula(exp);
        }

        private String[][] strings2arrays(List<List<String>> ms1Comments) {
            String[][] buf = new String[ms1Comments.size()][];
            for (int k=0; k < ms1Comments.size(); ++k) {
                buf[k] = ms1Comments.get(k).toArray(String[]::new);
            }
            return buf;
        }

        private void error(String s) throws IOException {
            throw new IOException(lineNumber + ": " + s);
        }

        private void warn(String msg) {
            log.warn(lineNumber + ": " + msg);
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
                    currentSpectrum.computeAnnotationIfAbsent(AdditionalFields.class, AdditionalFields::new).put(optionName, value);
                else
                    warn("Cannot parse spectrum level (##) comment (" + line + ") because there is no Spectrum it belongs to.");
            }
        }

        private double parseRetentionTimeMiddle(String value) {
            if (!currentSpectrum.isEmpty()) newSpectrum();
            final double p = parseRetentionTime(value);
            return Double.isNaN(p) ? 0 : p;
        }

        private double parseRetentionTime(String value) {
            final Matcher m = RETENTION_PATTERN.matcher(value);
            if (m.find()) {
                return Utils.parseDoubleWithUnknownDezSep(m.group(1));
            } else {
                warn("Cannot parse retention time: '" + value + "'");
                return Double.NaN;
            }
        }

        private double parseTime(String value) {
            final Matcher m = TIME_PATTERN.matcher(value);
            if (m.find()) {
                return Utils.parseDoubleWithUnknownDezSep(m.group(1));
            } else {
                return Double.NaN;
            }
        }

        private void parsePeak(String line, List<String> comments) throws IOException {
            final Matcher m = PEAK_PATTERN.matcher(line);
            if (m.find()) {
                currentSpectrum.getSourceSpectrum().addPeak(new SimplePeak(Utils.parseDoubleWithUnknownDezSep(m.group(1)), Utils.parseDoubleWithUnknownDezSep(m.group(2))));
                if (m.group(3)!=null && !m.group(3).isEmpty()) {
                    hasPeakComment = true;
                    comments.add(m.group(3).strip());
                } else comments.add(null);
            } else {
                error("Cannot parse peak '" + line + "'");
            }
        }

        private void newSpectrum() {
            //always parse empty MS1/MS2. else this might create issues with MS1/MS2 mapping
            final SpectrumWithAdditionalFields<Peak> spec;
            if (spectrumType == SPECTRUM_TYPE.MS1) {
                spec = new SimpleSpectrumWithAdditionalFields(currentSpectrum);
                ms1Comments.add(currentComments);
                ms1spectra.add((SimpleSpectrum) spec);
            } else if (spectrumType == SPECTRUM_TYPE.MS2) {
                spec = new MutableMs2SpectrumWithAdditionalFields(currentSpectrum, parentMass, currentEnergy, 2);
                ((MutableMs2Spectrum) spec).setTotalIonCount(tic);
                ms2spectra.add((MutableMs2Spectrum) spec);
                ms2Comments.add(currentComments);
            } else if (!currentSpectrum.isEmpty()) {
                if (spectrumType == SPECTRUM_TYPE.MERGED_MS1) {
                    mergedMs1 = new SimpleSpectrumWithAdditionalFields(currentSpectrum);
                    spec = mergedMs1;
                    mergedComments=currentComments;
                } else {
                    warn("Unknown spectrum type. Description must contain one of the following keywords '>[ms1|mergedms1|ms2|collision|energy]'. " +
                            "Spectrum will be processed as MS2 spectrum.");
                    spec = new MutableMs2SpectrumWithAdditionalFields(currentSpectrum, parentMass, currentEnergy, 2);
                    ms2spectra.add((MutableMs2Spectrum) spec);
                    ms2Comments.add(currentComments);
                }
            } else return;

            //transfer spectra to copied version of spectrum
            currentSpectrum.getAnnotation(AdditionalFields.class).ifPresent(
                    fields -> spec.additionalFields().putAll(fields)
            );

            //creat new spectrum
            spectrumType = SPECTRUM_TYPE.UNKNOWN;
            this.tic = 0;
            this.currentEnergy = null;
            this.currentSpectrum = AnnotatedWrapperSpectrum.of(new SimpleMutableSpectrum());
            currentComments = new ArrayList<>();
        }

        private void parseEmptyLine() {
            newSpectrum();
        }

        private MolecularFormula[] parseFormulas(String formulas) {
            if (formulas.contains(",")) {
                return Arrays.stream(formulas.split(",")).map(MolecularFormula::parseOrNull).filter(Objects::nonNull).toArray(MolecularFormula[]::new);
            } else {
                return new MolecularFormula[]{MolecularFormula.parseOrNull(formulas)};
            }
        }

        private void parseIonizations(String ions) throws IOException {
            if (ions.contains(",")) {
                changeConfig("AdductSettings.enforced", ions).ifPresent(key -> {
                    AdductSettings adducts = (AdductSettings) config.createInstanceWithDefaults(key);
                    if (adducts.getEnforced().stream().anyMatch(PrecursorIonType::isPositive) && adducts.getEnforced().stream().anyMatch(PrecursorIonType::isNegative))
                        warn("Adducts with positive and negative charge are given in the input '" + ions + "'. Choosing one charge randomly!");

                    this.ionization = PrecursorIonType.unknown(
                            adducts.getEnforced().stream().findAny().map(PrecursorIonType::getCharge).orElse(1));
                });
            } else {
                final PrecursorIonType ion = PeriodicTable.getInstance().ionByNameOrNull(ions.trim());
                if (ion == null) {
                    warn("Unknown ionization: '" + ions + "'");
                    return;
                }
                this.ionization = ion;
            }
        }
    }
}
