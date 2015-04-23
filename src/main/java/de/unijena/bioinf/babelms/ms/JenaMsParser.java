package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.babelms.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenaMsParser implements Parser<Ms2Experiment> {

    @Override
    public JenaMsExperiment parse(BufferedReader reader) throws IOException {
        return new ParserInstance(reader).parse();
    }

    private static class ParserInstance {

        private ParserInstance(BufferedReader reader) {
            this.reader = reader;
            lineNumber = 0;
            this.currentSpectrum = new SimpleMutableSpectrum();
        }

        private final BufferedReader reader;
        private int lineNumber;
        private String compoundName = null;
        private MolecularFormula formula;
        private int charge=0;
        private boolean isMs1 = false;
        private Ionization ionization;
        private CollisionEnergy currentEnergy;
        private double tic=0, parentMass=0, retentionTime=0;
        private SimpleMutableSpectrum currentSpectrum;
        private ArrayList<JenaMs2Spectrum> ms2spectra = new ArrayList<JenaMs2Spectrum>();
        private ArrayList<JenaMsSpectrum> ms1spectra = new ArrayList<JenaMsSpectrum>();

        private JenaMsExperiment parse() throws IOException {
            while (reader.ready()) {
                final String line = reader.readLine();
                try {
                    ++lineNumber;
                    if (line.isEmpty()) {
                        parseEmptyLine();
                    } else {
                        final char firstCharacter = line.charAt(0);
                        if (firstCharacter == '>') {
                            parseOption(line);
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
            parseEmptyLine();
            if (compoundName==null) return null;
            return new JenaMsExperiment(compoundName, formula, parentMass, charge, ionization, ms1spectra, ms2spectra);
        }

        private static Pattern LINE_PATTERN = Pattern.compile("^\\s*([>#]|\\d)");

        private static final String decimalPattern = "[+-]?\\s*\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?";

        private static final Pattern MASS_PATTERN = Pattern.compile("("+decimalPattern + ")(?:\\s*Da)?");

        private static final Pattern FLOAT_PATTERN = Pattern.compile("("+decimalPattern+")");

        private static final Pattern COLLISION_PATTERN = Pattern.compile("((" + decimalPattern + ")?|((?:Ramp\\s*)?" + decimalPattern + "\\s*-\\s*" + decimalPattern +"))");

        private static final Pattern RETENTION_PATTER = Pattern.compile("(?:PT)?(" + decimalPattern + ")S?");

        private static final Pattern PEAK_PATTERN = Pattern.compile("(" + decimalPattern + ")\\s+(" + decimalPattern + ")");

        private void parseOption(String line) throws IOException {
            final String[] options = line.substring(line.indexOf('>')+1).split("\\s+", 2);
            final String optionName = options[0].toLowerCase();
            final String value = options.length==2 ? options[1] : "";
            if (optionName.equals("compound")) {
                if (compoundName!=null) warn("Compound name is set twice");
                this.compoundName = value;
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
                    this.charge = (int)Double.parseDouble(m.group(1));
                } else {
                    error("Cannot parse charge '" + value + "'");
                }
            } else if (optionName.equals("collision")) {
                if (currentSpectrum.size()>0) newSpectrum();
                if (currentEnergy != null) warn("Collision energy is set twice");
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
            } else if (optionName.equals("tic")) {
                if (currentSpectrum.size()>0) newSpectrum();
                if (tic != 0) warn("total ion count is set twice");
                final Matcher m = FLOAT_PATTERN.matcher(value);
                if (m.find()) {
                    tic = Double.parseDouble(value);
                } else {
                    error("Cannot parse total ion count: '" + value + "'");
                }
            } else if (optionName.equals("ms1peaks")) {
                if (currentSpectrum.size()>0) newSpectrum();
                this.isMs1 = true;
            } else if (optionName.equals("retention")) {
                parseRetention(value);
            } else if (optionName.equals("ionization")) {
                final Ionization ion = PeriodicTable.getInstance().ionByName(value.trim());
                if (ion==null) {
                    warn("Unknown ionization: '" + value + "'");
                } else {
                    this.ionization = ion;
                }

            } else {
                warn("Unknown option '>" + optionName + "'");
            }
        }

        private void error(String s) throws IOException {
            throw new IOException(lineNumber + ": " + s);
        }

        private void warn(String msg) {
            System.err.println(lineNumber + ": " + msg);
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
                    ms1spectra.add(new JenaMsSpectrum(currentSpectrum, tic, retentionTime));
                } else {
                    ms2spectra.add(new JenaMs2Spectrum(currentSpectrum, parentMass, tic, currentEnergy, retentionTime));
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
