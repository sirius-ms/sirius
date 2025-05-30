
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

package de.unijena.bioinf.babelms.mgf;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.exceptions.MultimereException;
import de.unijena.bioinf.ChemistryBase.exceptions.MultipleChargeException;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.SpectralParser;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.babelms.utils.ParserUtils;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.unijena.bioinf.ChemistryBase.chem.InChIs.newInChI;

public class MgfParser extends SpectralParser implements Parser<Ms2Experiment> {
    private enum SpecType {
        UNKNOWN, MS1, MSMS, CORRELATED
    }

    private static class MgfSpec {

        private String featureId;
        private MutableMs2Spectrum spectrum;
        private PrecursorIonType ionType;
        private HashMap<String, String> fields;
        private String inchi, smiles, name;
        private RetentionTime retentionTime;
        private MolecularFormula formula;
        private MsInstrumentation instrumentation = MsInstrumentation.Unknown;
        private SpecType type;
        private FunctionalMetabolomics fmet = null;

        public MgfSpec(MgfSpec s) {
            this.spectrum = new MutableMs2Spectrum(s.spectrum);
            this.ionType = s.ionType;
            this.fields = new HashMap<>(s.fields);
            this.inchi = s.inchi;
            this.smiles = s.smiles;
            this.name = s.name;
            this.featureId = s.featureId;
            this.retentionTime = s.retentionTime;
            this.type = s.type;
            this.formula = null;
        }

        public MgfSpec() {
            this.spectrum = new MutableMs2Spectrum();
            this.fields = new HashMap<>();
            this.type = SpecType.UNKNOWN;
        }
    }

    private static class MgfParserInstance {
        private final MgfSpec prototype;
        private final ArrayDeque<MgfSpec> buffer;
        private final Object source;
        private final BufferedReader reader;
        private int specIndex = 0;
        protected boolean ignoreUnsupportedIonTypes;

        public MgfParserInstance(BufferedReader reader, Object source) {
            this(reader, source,false);
        }

        public MgfParserInstance(BufferedReader reader, Object source, boolean ignoreUnsupportedIonTypes) {
            this.reader = reader;
            this.source = source;
            this.prototype = new MgfSpec();
            this.prototype.spectrum = new MutableMs2Spectrum();
            this.buffer = new ArrayDeque<>();
            this.ignoreUnsupportedIonTypes = ignoreUnsupportedIonTypes;
        }

        public boolean hasNext() throws IOException {
            addNextEntry();
            return !buffer.isEmpty();
        }

        public MgfSpec peekNext() throws IOException {
            addNextEntry();
            return buffer.peekFirst();
        }

        public MgfSpec pollNext() throws IOException {
            addNextEntry();
            return buffer.pollFirst();
        }

        private void addNextEntry() throws IOException {
            if (!buffer.isEmpty()) return;
            MgfSpec s = readNext();
            if (s != null)
                buffer.addLast(s);
        }

        private static final Pattern CHARGE_PATTERN = Pattern.compile("([+-]?)(\\d*)([+-]?)"); //Group 1: optional +-, Group 2: 0,1 or multiple digits, Group 3: optional +-
        private static final Pattern NOT_AVAILABLE = Pattern.compile("\\s*N/A\\s*");

        private void handleKeyword(MgfSpec spec, String keyword, String value) throws IOException {
            keyword = keyword.toUpperCase();
            value = value.trim();
            if (value.isEmpty()) return;
            if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                value = value.substring(1, value.length() - 1);
            if (keyword.equals("PEPMASS")) {
                spec.spectrum.setPrecursorMz(Double.parseDouble(value.split("\\s+")[0]));
            } else if (keyword.startsWith("FEATURE_ID")) {
                spec.featureId = value;
            } else if (keyword.contains("RTINSECONDS")) {
                final String[] parts = value.split("-");
                if (parts.length == 1 || parts[0].isEmpty()) {
                    spec.retentionTime = new RetentionTime(Double.parseDouble(parts[parts.length - 1]));
                } else {
                    double a = Double.parseDouble(parts[0]), b = Double.parseDouble(parts[1]);
                    spec.retentionTime = new RetentionTime(a, b);
                }
            } else if (keyword.equals("SOURCE_INSTRUMENT")) {
                for (MsInstrumentation.Instrument i : MsInstrumentation.Instrument.values()) {
                    if (i.isInstrument(value)) {
                        spec.instrumentation = i;
                        break;
                    }
                }
            } else if (keyword.contains("FORMULA")) {
                spec.formula = MolecularFormula.parseOrNull(value);
                if (spec.formula == null)
                    LoggerFactory.getLogger(MgfParser.class).warn("Cannot parse molecular formula '" + value + "'. Ignore field.");
            } else if (keyword.equals("CHARGE")) {
                final Matcher m = CHARGE_PATTERN.matcher(value);
                m.find();

                int charge = !m.group(2).isEmpty() ? Integer.parseInt(m.group(2)) : 0; //If no digit is found, assign 0 charge
                if (charge == 0) {
                    charge = 1;
                    LoggerFactory.getLogger(MgfParser.class).warn("'CHARGE' value of 0 found. Changing value to Single charged under consideration of the sign.");
                }
                if ("-".equals(m.group(3)) || "-".equals(m.group(1)))
                    charge = Math.abs(charge) * -1;


                if (spec.spectrum.getIonization() == null || spec.spectrum.getIonization().getCharge() != charge)
                    spec.spectrum.setIonization(new Charge(charge));
                if (spec.ionType == null) spec.ionType = PrecursorIonType.unknown(charge);
            } else if (keyword.startsWith("ION") || keyword.contains("ADDUCT")) {
                final PrecursorIonType ion;
                final Matcher cm = CHARGE_PATTERN.matcher(value);
                if (value.toLowerCase().startsWith("pos")) {
                    ion = PrecursorIonType.unknown(1);
                } else if (value.toLowerCase().startsWith("neg")) {
                    ion = PrecursorIonType.unknown(-1);
                } else if (cm.matches()) {
                    int charge = Integer.parseInt(cm.group(2));
                    if (charge == 0) {
                        charge = 1;
                        LoggerFactory.getLogger(MgfParser.class).warn("'" + keyword + "' has Charge value of 0. Changing value to Single charged under consideration of the sign.");
                    }
                    if ("-".equals(cm.group(1)) || "-".equals(cm.group(3)))
                        charge = charge * -1;
                    ion = PrecursorIonType.unknown(charge);
                } else {
                    try {
                        ion = PeriodicTable.getInstance().ionByNameOrNull(value);
                        if (ion == null) {
                            LoggerFactory.getLogger(this.getClass()).error("Unknown ion '" + value + "'");
                            if (!ignoreUnsupportedIonTypes) throw new IOException("Unknown ion '" + value + "'");
                            else return;
                        }
                    } catch (MultipleChargeException | MultimereException e) {
                        LoggerFactory.getLogger(this.getClass()).warn(e.getMessage());
                        if (!ignoreUnsupportedIonTypes) throw (e);
                        else return;
                    } catch (RuntimeException e) {
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                        if (!ignoreUnsupportedIonTypes) throw (e);
                        else return;
                    }
                }
                if (spec.ionType == null || spec.ionType.isIonizationUnknown()) {
                    spec.spectrum.setIonization(ion.getIonization());
                    spec.ionType = ion;
                } else if (!ion.isIonizationUnknown() && !spec.ionType.equals(ion.isIonizationUnknown())) {
                    LoggerFactory.getLogger(MgfParser.class).warn("Contradicting adduct annotations: " + ion + " vs " + spec.ionType);
                }
            } else if (keyword.contains("SPECTYPE")) {
                if (value.toUpperCase().contains("CORRELATED")) {
                    spec.type = SpecType.CORRELATED;
                }
            } else if (keyword.contains("LEVEL")) {
                final int level = Integer.parseInt(value);
                spec.spectrum.setMsLevel(level);
                if (level == 1 && spec.type != SpecType.CORRELATED) spec.type = SpecType.MS1;
                else if (level > 1) spec.type = SpecType.MSMS;
            } else if (keyword.equalsIgnoreCase("ONLINE_REACTIVITY")) {
                spec.fmet = FunctionalMetabolomics.parseOnlineReactivityFromMZMineFormat(value);
            } else {
                if (NOT_AVAILABLE.matcher(value).matches()) return;
                if (keyword.equalsIgnoreCase("INCHI")) {
                    if (!value.equalsIgnoreCase("n/a") && !value.equalsIgnoreCase("na")) {
                        spec.inchi = value;
                    }
                } else if (keyword.equalsIgnoreCase("SMILES")) {
                    if (!value.equalsIgnoreCase("n/a") && !value.equalsIgnoreCase("na")) {
                        spec.smiles = value;
                    }
                } else if (keyword.equalsIgnoreCase("SCANS")){
                    if(spec.name==null){ //Only use SCANS field if actual fields dont exist, if they do they overwrite
                        spec.name=value;
                    }
                }

                else if (keyword.equalsIgnoreCase("NAME") || keyword.equalsIgnoreCase("TITLE")) {
                    spec.name = value;
                } else {
                    spec.fields.put(keyword, value);
                }
            }
        }

        private String lastErrorFeatureId = null;

        private MgfSpec readNext() throws IOException {
            String line;
            boolean reading = false;
            MgfSpec spec = null;
            while ((line = reader.readLine()) != null) {
                try {
                    if (line.isEmpty()) continue;
                    if (!reading && line.startsWith("BEGIN IONS")) {
                        spec = new MgfSpec(prototype);
                        reading = true;
                    } else if (reading && line.startsWith("END IONS")) {
                        lastErrorFeatureId = null;
                        checkSpecIntegrity(spec);
                        return spec;
                    } else if (reading) {
                        if (Character.isDigit(line.charAt(0))) {
                            final String[] parts = line.split("\\s+");
                            spec.spectrum.addPeak(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                        } else {
                            final int i = line.indexOf('=');
                            if (i >= 0) handleKeyword(spec, line.substring(0, i), line.substring(i + 1));
                        }
                    } else {
                        final int i = line.indexOf('=');
                        if (i >= 0) handleKeyword(prototype, line.substring(0, i), line.substring(i + 1));
                    }
                } catch (RuntimeException e) {
                    //increase index for not-parsed compounds.
                    boolean increasedIndex = false;
                    if (spec.featureId != null && !spec.featureId.equals(lastErrorFeatureId)) {
                        ++specIndex;
                        increasedIndex = true;
                        lastErrorFeatureId = spec.featureId;
                    }

                    if (e instanceof MultipleChargeException || e instanceof MultimereException) {
                        LoggerFactory.getLogger(this.getClass()).warn("Compound " + lastErrorFeatureId + " ignored because of: " + e.getMessage());
                    } else {
                        LoggerFactory.getLogger(this.getClass()).error("Compound " + lastErrorFeatureId + " ignored because of: " + e.getMessage(), e);
                    }

                    if (reading) {
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("END IONS")) {
                                reading = false;
                                break;
                            } else if (line.startsWith("BEGIN IONS")) {
                                reading = true;
                                spec = new MgfSpec(prototype);
                                break;
                            } else if (!increasedIndex && line.toUpperCase().startsWith("FEATURE_ID")) {
                                final int i = line.indexOf('=');
                                String id = line.substring(i + 1).trim();
                                if (!id.isEmpty() && !id.equals(lastErrorFeatureId)) {
                                    ++specIndex;
                                    increasedIndex = true;
                                    lastErrorFeatureId = id;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        private void checkSpecIntegrity(MgfSpec spec) {
            if (spec.ionType == null) throw new RuntimeException("Ion type is not defined");
        }
    }

    @Override
    public Iterator<Ms2Spectrum<Peak>> parseSpectra(final BufferedReader reader) {
        return new Iterator<>() {

            private final MgfParserInstance inst = new MgfParserInstance(reader, reader);

            @Override
            public boolean hasNext() {
                try {
                    return inst.hasNext();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Ms2Spectrum<Peak> next() {
                try {
                    if (!inst.hasNext()) throw new NoSuchElementException();
                    return inst.pollNext().spectrum;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private MgfParserInstance inst;

    @Override
    public synchronized Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        if (inst == null || inst.source != inputStream) inst = new MgfParserInstance(FileUtils.ensureBuffering(new InputStreamReader(inputStream)), inputStream);
        return parseInst(source);
    }

    @Override
    public synchronized Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        if (inst == null || inst.source != reader) inst = new MgfParserInstance(reader, reader);
        return parseInst(source);
    }

    private synchronized Ms2Experiment parseInst(URI source) throws IOException {
        if (!inst.hasNext())
            return null;

        ++inst.specIndex;
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs2Spectra(new ArrayList<>());
        exp.setMs1Spectra(new ArrayList<>());
        exp.setIonMass(inst.peekNext().spectrum.getPrecursorMz());
        exp.setName(inst.peekNext().name);
        exp.setFeatureId(inst.peekNext().featureId);
        if (exp.getName() == null)
            exp.setName(exp.getFeatureId());
        if (exp.getName() == null) {
            exp.setName("FEATURE_" + inst.specIndex);
        }
        exp.computeAnnotationIfAbsent(AdditionalFields.class, AdditionalFields::new).put("index", Integer.toString(inst.specIndex));
        final AdditionalFields additionalFields = new AdditionalFields();

        //if some annotation should be persistent in nitrite it must be added to this config. additionalFields are not persistent
        //but in contrast to JenaMsParser, mgf is not meant to support our parameters
        ParameterConfig config = PropertyManager.DEFAULTS.newIndependentInstance("MS_FILE:" + exp.getName());

        while (true) {
            final MgfSpec spec = inst.pollNext();
            if (spec.spectrum.getMsLevel() == 1) {
                if (spec.type == SpecType.CORRELATED) {
                    exp.setMergedMs1Spectrum(new SimpleSpectrum(spec.spectrum));
                } else {
                    exp.getMs1Spectra().add(new SimpleSpectrum(spec.spectrum));
                }
            } else {
                exp.getMs2Spectra().add(new MutableMs2Spectrum(spec.spectrum));
            }
            if (exp.getPrecursorIonType() == null || exp.getPrecursorIonType().isIonizationUnknown()) {
                exp.setPrecursorIonType(spec.ionType);
                if (!spec.ionType.isIonizationUnknown())
                    exp.setAnnotation(DetectedAdducts.class, DetectedAdducts.singleton(DetectedAdducts.Source.INPUT_FILE, spec.ionType));
            }
            if (spec.fmet!=null) {
                try {
                    config.changeConfig("FunctionalMetabolomics", spec.fmet.toString());
                } catch (Throwable e) {
                    LoggerFactory.getLogger(MgfParser.class).error("Could not parse Config key = FunctionalMetabolomics with value = " + spec.fmet.toString()  + ".");
                }
            }
            if (spec.inchi != null && spec.inchi.startsWith("InChI=")) {
                exp.setAnnotation(InChI.class, newInChI(null, spec.inchi));
            }
            if (spec.smiles != null) {
                exp.setAnnotation(Smiles.class, new Smiles(spec.smiles));
            }
            if (spec.retentionTime != null) {
                if (exp.hasAnnotation(RetentionTime.class)) {
                    exp.setAnnotation(RetentionTime.class, exp.getAnnotationOrThrow(RetentionTime.class).merge(spec.retentionTime));
                } else {
                    exp.setAnnotation(RetentionTime.class, spec.retentionTime);
                }
            }
            if (spec.formula != null) {
                exp.setMolecularFormula(spec.formula); //this is for backwards compatibility with any old code / evaluation scripts etc. this should not be necessary for SIRIUS frontend application
                try {
                    config.changeConfig("CandidateFormulas", spec.formula.toString());
                } catch (Exception e) {
                    LoggerFactory.getLogger(MgfParser.class).error("Could not set given formula as whitelist in configuration: "+spec.formula, e);
                }
            }
            if (spec.instrumentation != null) {
                if (exp.hasAnnotation(MsInstrumentation.class)) {
                    if (spec.instrumentation != MsInstrumentation.Unknown)
                        exp.setAnnotation(MsInstrumentation.class, spec.instrumentation);
                } else {
                    exp.setAnnotation(MsInstrumentation.class, spec.instrumentation);
                }
            }
            additionalFields.putAll(spec.fields);

            //add config annotations that have been set within the file
            exp.setAnnotation(InputFileConfig.class, new InputFileConfig(config)); //set map for reconstructability
            exp.setAnnotationsFrom(config.createInstancesWithModifiedDefaults(Ms2ExperimentAnnotation.class, true));

            if (inst.hasNext()) {
                final MgfSpec nextOne = inst.peekNext();
                if (spec.featureId != null && !spec.featureId.equals(nextOne.featureId)) break;
                if (spec.name != null && spec.featureId == null && !spec.name.equals(nextOne.name)) break;
                if (exp.getPrecursorIonType() != null && !exp.getPrecursorIonType().isIonizationUnknown() && nextOne.ionType != null && !nextOne.ionType.isIonizationUnknown() && !exp.getPrecursorIonType().equals(nextOne.ionType))
                    break;
                if (nextOne.spectrum.getPrecursorMz() != 0) {
                    if ((spec.featureId != null || spec.name != null)) {
                        if (Math.abs(nextOne.spectrum.getPrecursorMz() - exp.getIonMass()) > 0.005) break;
                    } else if (nextOne.spectrum.getPrecursorMz() != exp.getIonMass()) break;
                }
            } else break;
        }

        ParserUtils.checkMolecularFormula(exp);

        if (!additionalFields.isEmpty())
            exp.setAnnotation(AdditionalFields.class, additionalFields);

        exp.setAnnotation(SpectrumFileSource.class, new SpectrumFileSource(source));
        return exp;
    }
}
