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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.Parser;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenaMsParser implements Parser<Ms2Experiment> {

    // quickn dirty hack
    BufferedReader lastReader=null;
    String lastCompundName=null;

    @Override
    public Ms2Experiment parse(BufferedReader reader, URL source) throws IOException {
        ParserInstance p=null;
        try {
        if (reader==lastReader) {
            p = new ParserInstance(source, reader);
            p.newCompound(lastCompundName);
            return p.parse();
        } else {
            p = new ParserInstance(source, reader);
            return p.parse();
        }}finally {
            if (p!=null) {
                if (p.compoundName!=null) {
                    lastReader = reader;
                }
                lastCompundName = p.compoundName;
            }
        }
    }

    private static class ParserInstance {

        private ParserInstance(URL source, BufferedReader reader) {
            this.source = source;
            this.reader = reader;
            lineNumber = 0;
            this.currentSpectrum = new SimpleMutableSpectrum();
        }

        private final URL source;
        private final BufferedReader reader;
        private int lineNumber;
        private String compoundName = null;
        private MolecularFormula formula;
        private int charge=0;
        private boolean isMs1 = false;
        private PrecursorIonType ionization;
        private CollisionEnergy currentEnergy;
        private double tic=0, parentMass=0, retentionTime=0;
        private SimpleMutableSpectrum currentSpectrum;
        private ArrayList<MutableMs2Spectrum> ms2spectra = new ArrayList<MutableMs2Spectrum>();
        private ArrayList<SimpleSpectrum> ms1spectra = new ArrayList<SimpleSpectrum>();
        private String inchi, inchikey, smiles, splash;
        private MutableMs2Experiment experiment;

        private void newCompound(String name) {
            inchi=null; inchikey=null; smiles=null; splash=null;
            ms1spectra = new ArrayList<>();
            ms2spectra = new ArrayList<>();
            tic=0; parentMass=0; retentionTime=0;
            currentEnergy = null;
            ionization = null;
            isMs1=false;
            charge=0;
            formula=null;
            compoundName = name;
        }

        private MutableMs2Experiment parse() throws IOException {
            String line;
            while ((line=reader.readLine())!=null) {
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
                                    case '>': parseOption(line.trim()); break;
                                    case '#': parseComment(line.trim()); break;
                                    default: parsePeak(line.trim());
                                }
                            } else if (line.trim().isEmpty()) {
                                parseEmptyLine();
                            } else {
                                throw new IOException("Cannot parse line " + lineNumber + ":'" + line + "'" );
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    error(e.toString());
                }
            }
            flushCompound();
            return experiment;
        }

        private static Pattern LINE_PATTERN = Pattern.compile("^\\s*([>#]|\\d)");

        private static final String decimalPattern = "[+-]?\\s*\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?";

        private static final Pattern MASS_PATTERN = Pattern.compile("("+decimalPattern + ")(?:\\s*Da)?");

        private static final Pattern FLOAT_PATTERN = Pattern.compile("("+decimalPattern+")");

        private static final Pattern COLLISION_PATTERN = Pattern.compile("((" + decimalPattern + ")?|((?:Ramp\\s*)?" + decimalPattern + "\\s*-\\s*" + decimalPattern +"))");

        private static final Pattern RETENTION_PATTER = Pattern.compile("(?:PT)?(" + decimalPattern + ")S?");

        private static final Pattern PEAK_PATTERN = Pattern.compile("(" + decimalPattern + ")\\s+(" + decimalPattern + ")");

        private boolean parseOption(String line) throws IOException {
            final String[] options = line.substring(line.indexOf('>')+1).split("\\s+", 2);
            final String optionName = options[0].toLowerCase();
            final String value = options.length==2 ? options[1] : "";
            if (optionName.equals("compound")) {
                final boolean newCompound = compoundName!=null;
                if (newCompound) {
                    flushCompound();
                }
                newCompound(value);
                if (newCompound) return true;

            } else if (optionName.equals("formula")) {
                if (formula != null) warn("Molecular formula is set twice");
                this.formula = MolecularFormula.parse(value);
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
                    inchi = value;
                }
            } else if (optionName.equalsIgnoreCase("inchikey")) {
                inchikey = value;
            } else if (optionName.equalsIgnoreCase("smarts") || optionName.equalsIgnoreCase("smiles")) {
                smiles = value;
            } else if (optionName.equalsIgnoreCase("splash")) {
                splash = value;
            } else if (optionName.contains("collision") || optionName.contains("energy") || optionName.contains("ms2")) {
                if (currentSpectrum.size()>0) newSpectrum();
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
                if (currentSpectrum.size()>0) newSpectrum();
                if (tic != 0) warn("total ion count is set twice");
                final Matcher m = FLOAT_PATTERN.matcher(value);
                if (m.find()) {
                    tic = Double.parseDouble(value);
                } else {
                    error("Cannot parse total ion count: '" + value + "'");
                }
            } else if (optionName.contains("ms1")) {
                if (currentSpectrum.size()>0) newSpectrum();
                this.isMs1 = true;
            } else if (optionName.equals("retention")) {
                parseRetention(value);
            } else if (optionName.contains("ion")) {
                final PrecursorIonType ion = PeriodicTable.getInstance().ionByName(value.trim());
                if (ion==null) {
                    warn("Unknown ionization: '" + value + "'");
                } else {
                    this.ionization = ion;
                }

            } else {
                warn("Unknown option '>" + optionName + "'");
            }
            return false;
        }

        private void flushCompound() {
            experiment = null;
            if (compoundName==null) return;
            final MutableMs2Experiment exp = new MutableMs2Experiment();
            exp.setIonMass(parentMass);
            if (parentMass!=0 && ionization!=null)
                exp.setMoleculeNeutralMass(ionization.precursorMassToNeutralMass(exp.getIonMass()));
            exp.setMolecularFormula(formula);
            exp.setName(compoundName);
            if (ionization==null) {
                if (charge!=0) {
                    exp.setPrecursorIonType(PrecursorIonType.unknown(charge));
                }
            } else  {
                exp.setPrecursorIonType(ionization);
            }
            exp.setMs1Spectra(ms1spectra);
            exp.setMs2Spectra(ms2spectra);
            exp.setSource(source);
            if (smiles!=null) exp.setAnnotation(Smiles.class, new Smiles(smiles));
            if (splash!=null) exp.setAnnotation(Splash.class, new Splash(splash));
            if (inchi!=null || inchikey != null) exp.setAnnotation(InChI.class, new InChI(inchikey, inchi));
            this.experiment = exp;
            this.compoundName = null;
        }

        private void error(String s) throws IOException {
            throw new IOException(lineNumber + ": " + s);
        }

        private void warn(String msg) {
            LoggerFactory.getLogger(this.getClass()).error(lineNumber + ": " + msg);
        }

        private void parseComment(String line) {
            if (line.startsWith("#retention")) {
                parseRetention(line.substring(line.indexOf("#retention")+"#retention".length()));
            }
        }

        private void parseRetention(String value) {
            if (currentSpectrum.size()>0) newSpectrum();
            final Matcher m = RETENTION_PATTER.matcher(value);
            if (m.find()) {
                this.retentionTime = Double.parseDouble(m.group(1));
            } else {
                warn("Cannot parse retention time: '" + value + "'");
            }
        }

        private void parsePeak(String line) throws IOException {
            final Matcher m = PEAK_PATTERN.matcher(line);
            if (m.find()) {
                currentSpectrum.addPeak(new Peak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))));
            } else {
                error("Cannot parse peak '" + line + "'" );
            }
        }

        private void newSpectrum() {
            if (currentSpectrum.size() > 0) {
                if (isMs1) {
                    ms1spectra.add(new SimpleSpectrum(currentSpectrum));
                } else {
                    ms2spectra.add(new MutableMs2Spectrum(currentSpectrum, parentMass, currentEnergy, 2));
                }
            } else return;
            isMs1=false;
            this.tic=0;
            this.retentionTime=0d;
            this.currentEnergy=null;
            this.currentSpectrum=new SimpleMutableSpectrum();
        }

        private void parseEmptyLine() {
            newSpectrum();
        }

    }
}
