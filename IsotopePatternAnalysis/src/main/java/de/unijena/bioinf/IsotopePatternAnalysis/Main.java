package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FinestructurePatternGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        try {
            test();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void test() throws IOException {
        final File f = new File("D:/daten/arbeit/sessions/iso/ms1IsotopePatterns.txt");
        final BufferedReader r = new BufferedReader(new FileReader(f));
        String line = null;
        final List<MolecularFormula> formulas = new ArrayList<MolecularFormula>();
        final List<SimpleSpectrum> spectra = new ArrayList<SimpleSpectrum>();
        SimpleMutableSpectrum current = null;
        final Normalization norm = Normalization.Sum(1d);
        while ((line = r.readLine()) != null) {
            if (line.startsWith("C")) {
                formulas.add(MolecularFormula.parse(line.trim()));
                if (current != null) spectra.add(Spectrums.getNormalizedSpectrum(current, norm));
                current = new SimpleMutableSpectrum();
            } else if (line.length() > 0) {
                final String[] peak = line.split(" ");
                current.addPeak(Double.parseDouble(peak[0]), Double.parseDouble(peak[1]));
            }
        }
        if (current != null) spectra.add(Spectrums.getNormalizedSpectrum(current, norm));
        final FastIsotopePatternGenerator gen1 = new FastIsotopePatternGenerator();
        gen1.setMaximalNumberOfPeaks(8);
        gen1.setMinimalProbabilityThreshold(1e-8);
        final FinestructurePatternGenerator gen2 = new FinestructurePatternGenerator();
        gen2.setMaximalNumberOfPeaks(8);
        gen2.setMinimalProbabilityThreshold(1e-8);
        gen2.setResolution(7500);
        final Ionization hplus = PeriodicTable.getInstance().ionByName("[M+H]+");

        double[] avg1sum = new double[3];
        double[] avg2sum = new double[3];
        double[] int1sum = new double[3];
        double[] int2sum = new double[3];
        int[] count = new int[3];

        for (int k = 0; k < formulas.size(); ++k) {
            final SimpleSpectrum spec = spectra.get(k);
            gen1.setMaximalNumberOfPeaks(spec.size());
            gen2.setMaximalNumberOfPeaks(spec.size());
            final MolecularFormula formula = formulas.get(k);
            final SimpleSpectrum s1 = gen1.simulatePattern(formula, hplus);
            if (Math.abs(s1.getMzAt(0) - spec.getMzAt(0)) > 0.01) continue;
            final SimpleSpectrum s2 = gen2.simulatePattern(formula, hplus);

            for (int x = 1; x < Math.min(s1.size(), s2.size()); ++x) {
                final double mz1 = (s1.getMzAt(x) - s1.getMzAt(0)) - (spec.getMzAt(x) - spec.getMzAt(0));
                final double ppm1 = mz1 * 1e6 / spec.getMzAt(x);
                final double int1 = s1.getIntensityAt(x) - spec.getIntensityAt(x);

                final double mz2 = (s2.getMzAt(x) - s2.getMzAt(0)) - (spec.getMzAt(x) - spec.getMzAt(0));
                final double ppm2 = mz2 * 1e6 / spec.getMzAt(x);
                final double int2 = s2.getIntensityAt(x) - spec.getIntensityAt(x);

                if (x <= 3) {
                    avg1sum[x - 1] += Math.abs(ppm1);
                    avg2sum[x - 1] += Math.abs(ppm2);
                    ++count[x - 1];
                    int1sum[x - 1] += Math.abs(int1);
                    int2sum[x - 1] += Math.abs(int2);
                }

                System.out.println(String.format(Locale.US, "SIRIUS:\t%.5f (%.3f ppm)\tFINESTRUCT:\t%.5f (%.3f ppm)", mz1, ppm1, mz2, ppm2));
                System.out.println(String.format(Locale.US, "SIRIUS:\t%.3f %%\tFINESTRUCT:\t%.3f %%", int1 * 100, int2 * 100));
            }

        }

        System.out.println("############# AVERAGE ############");
        for (int k = 0; k < count.length; ++k) {
            System.out.println("-- Peak: " + (k + 1));
            System.out.println(String.format(Locale.US, "SIRIUS:\t%.8f ppm\tFINESTRUCT:\t%.8f ppm", avg1sum[k] / count[k], avg2sum[k] / count[k]));
            System.out.println(String.format(Locale.US, "SIRIUS:\t%.6f %%\tFINESTRUCT:\t%.6f %%", int1sum[k] * 100d / count[k], int2sum[k] * 100d / count[k]));
        }


    }

}
