package de.unijena.bioinf.IsotopePatternAnalysis.isogencli;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistributionJSONFile;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private final static String VERSION = "2.0";

    private final static String CITATION = "SIRIUS: decomposing isotope patterns for metabolite identification\n" +
            "Sebastian Böcker, Matthias C. Letzel, Zsuzsanna Lipták and Anton Pervukhin\n" +
            "Bioinformatics (2009) 25 (2): 218-224";

	public static void main(String[] args) {
        final Cli<Options> cli = CliFactory.createCli(Options.class);
        try {
            final Options options = cli.parseArguments(args);
            if (options.getVersion())  {
                System.out.println("Isotopic Pattern Analysis " + VERSION + "\n" + CITATION);
            } else {

                final String dist = options.getIsotopeDistributionFile();
                if (dist != null) {
                    final File f = new File(dist);
                    final IsotopicDistribution distribution;
                    try {
                        if (f.exists() && f.canRead()) {
                            final FileReader r = new FileReader(f);
                            try{
                                distribution = new IsotopicDistributionJSONFile(r);
                            } finally {
                                r.close();
                            }
                        } else if (dist.equalsIgnoreCase("chemcalc")) {
                            distribution = IsotopicDistribution.loadChemcalc2012();
                        } else {
                            distribution = IsotopicDistributionJSONFile.fromClassPath(dist);
                        }
                    } catch (IOException e) {
                        System.err.print("Error while reading '" + f.getPath() + "': ");
                        System.err.println(e.getMessage());
                        return;
                    }
                    IsotopicDistribution.setInstance(distribution);
                }

                final MolecularFormula formula = MolecularFormula.parse(options.getMolecularFormula());
                final Normalization normalization = new Normalization(NormalizationMode.valueOf(options.getNormalization().toUpperCase()),
                        options.getScalingFactor());
                final String ionizationMode = options.getIonizationMode() == null ? null : options.getIonizationMode().trim();
                final Ionization ionization;
                if (ionizationMode != null) {
                    final Pattern isNum = Pattern.compile("^\\d+$");
                    final Pattern isSpecified = Pattern.compile("^\\[\\s*M\\s*([+-])\\s*(\\d+(\\.\\d*)?)\\s*\\]\\s*(\\d*)\\s*([+-])$");
                    final Matcher m = isNum.matcher(ionizationMode);
                    final Matcher m2 = isSpecified.matcher(ionizationMode);
                    if (m.find()) {
                        final int charge = Integer.parseInt(m.group(1));
                        ionization = new Charge(charge);
                    } else if (m2.find()){
                        final String ioF = m2.group();
                        final int signMass = m2.group(1).charAt(0) == '-' ? -1 : +1;
                        final double adductMass = Double.parseDouble(m2.group(2)) * signMass;
                        final String chargeNumStr = m2.group(3);
                        final int chargeNum = (chargeNumStr != null && !chargeNumStr.isEmpty()) ? Integer.parseInt(chargeNumStr) : 1;
                        final int chargeSign = m2.group(4).charAt(0) == '-' ? -1 : +1;
                        final int charge = chargeNum*chargeSign;
                        ionization = new Ionization() {
                            @Override
                            public double getMass() {
                                return adductMass;
                            }

                            @Override
                            public int getCharge() {
                                return charge;
                            }

                            @Override
                            public String getFormula() {
                                return ioF;
                            }
                        };
                    } else {
                        final Ionization ion = PeriodicTable.getInstance().ionByName(ionizationMode);
                        if (ion == null) {
                            System.err.println("Unknown ion type " + ionizationMode);
                            System.err.println("Please set ionization explicitely: isogen -i \"[M+<mass>]<charge>+\"");
                            return;
                        } else ionization = ion;
                    }
                } else {
                    ionization = new Charge(1);
                }

                final double treshold;
                final int limit;
                final Spectrum<?> spectrum;
                final PatternGenerator generator = new PatternGenerator(ionization, normalization);
                if (options.getIntensityTreshold() == null && options.getNumberOfIsotopePeaks() == null) {
                    treshold = 0.01d;
                    limit = Integer.MAX_VALUE;
                    spectrum = generator.generatePatternWithTreshold(formula, treshold);
                } else {
                    treshold = (options.getIntensityTreshold() == null) ? 1e-16 : options.getIntensityTreshold()/100d;
                    limit = (options.getNumberOfIsotopePeaks() == null) ? Integer.MAX_VALUE : options.getNumberOfIsotopePeaks();
                    if (options.getIntensityTreshold() != null) {
                        final Spectrum<?> s = generator.generatePatternWithTreshold(formula, treshold);
                        final MutableSpectrum<?> t = new SimpleMutableSpectrum(s);
                        Spectrums.sortSpectrumByDescendingIntensity(t);
                        for (int k=t.size()-1; k >= limit; --k) {
                            t.removePeakAt(k);
                        }
                        Spectrums.sortSpectrumByMass(t);
                        spectrum = new SimpleMutableSpectrum(t);
                    } else {
                        spectrum = generator.generatePattern(formula, limit);
                    }
                }
                // output
                final SimpleMutableSpectrum s = new SimpleMutableSpectrum(spectrum);
                System.out.println("mass,intensity");
                for (Peak p : s) {
                    System.out.println(p.getMass() + "," + p.getIntensity());
                }
            }
        } catch (ArgumentValidationException e) {
            System.out.println(e.getMessage());
            if (!(e instanceof HelpRequestedException)) {
                System.out.println(cli.getHelpMessage());
            }
            System.out.println("Example:\nisogen -l 4 C6H12O6 > pattern.csv");
            return;
        }

	}
	
}
