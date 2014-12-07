package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.ArrayWrapperSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class FastIsotopePatternGenerator extends IsotopePatternGenerator {

    public FastIsotopePatternGenerator(IsotopicDistribution distribution, Normalization mode) {
        super(distribution, mode);
    }

    public FastIsotopePatternGenerator() {
    }

    public FastIsotopePatternGenerator(Normalization mode) {
        super(mode);
    }

    private static boolean isBitSet(int input, int pos) {
        if (pos >= Integer.SIZE) {
            return false;
        }
        return (input & (1 << pos)) > 0;
    }

    @Override
    public SimpleSpectrum simulatePattern(MolecularFormula formula, Ionization ion) {
        final MolecularFormula adduct = ion.getAtoms();
        final double diff;
        if (adduct != null && adduct.getIntMass() > 0) {
            formula = formula.add(adduct);
            diff = ion.getMass() - adduct.getMass();
        } else diff = ion.getMass();
        final SimpleMutableSpectrum spec = foldFormula(formula);
        final double mono = formula.getIntMass();
        for (int k = 0; k < spec.size(); ++k) {
            spec.setMzAt(k, k + spec.getMzAt(k) + mono + diff);
        }
        return new SimpleSpectrum(spec);
    }

    protected SimpleMutableSpectrum foldFormula(MolecularFormula formula) {
        ArrayWrapperSpectrum candidateDistribution = null;
        for (Element e : formula) {
            final Isotopes iso = distribution.getIsotopesFor(e);
            final int monoIsotopicMass = iso.getIntegerMass(0);
            int maxMass = iso.getIntegerMass(iso.getNumberOfIsotopes() - 1) - monoIsotopicMass;
            final int n = Math.max(iso.getNumberOfIsotopes() - 1, maxMass);
            final double[] modIsoMz = new double[n + 1];
            final double[] modIsoInt = new double[n + 1];
            ArrayWrapperSpectrum modIsoDist = new ArrayWrapperSpectrum(modIsoMz, modIsoInt);
            int k = 0;
            for (int i = 0; i <= n; i++) {
                int diff = iso.getIntegerMass(k) - monoIsotopicMass;
                while (diff > i) {
                    modIsoMz[i] = 0;
                    modIsoInt[i] = 0;
                    ++i;
                }
                // Florian says: minus i is because the i-th isotope nominal mass is elemental nominal mass plus i!
                modIsoMz[i] = iso.getMass(k) - e.getIntegerMass() - i;
                modIsoInt[i] = iso.getAbundance(k);
                ++k;
            }

            //get the reverse binary string of the quantity of an element
            int exp = formula.numberOf(e),
                    expLength = Integer.SIZE - Integer.numberOfLeadingZeros(exp);

            //folding of one element
            ArrayWrapperSpectrum helper = modIsoDist;
            ArrayWrapperSpectrum list = null;

            //if the first number of the binary exponent is 1,
            if (isBitSet(exp, 0)) {
                list = helper;
            }

            //helper list is always folded twice
            //list is just folded if binary exponent is 1 at the current position
            for (int i = 1; i < expLength; i++) {
                helper = fold(helper, helper);
                if (isBitSet(exp, i)) {
                    list = fold(list, helper);
                }
            }
            // folding all elements to the candidate peaks
            // fold returns only list if candidatePeaks is still null
            candidateDistribution = fold(candidateDistribution, list);
        }
        final SimpleMutableSpectrum finalSpectrum = new SimpleMutableSpectrum(candidateDistribution);
        for (int k = finalSpectrum.size() - 1; k >= 0; --k) {
            if (finalSpectrum.getIntensityAt(k) < minimalProbabilityThreshold) {
                finalSpectrum.removePeakAt(k);
            }
        }
        Spectrums.normalize(finalSpectrum, mode);
        return finalSpectrum;
    }

    protected ArrayWrapperSpectrum fold(ArrayWrapperSpectrum left, ArrayWrapperSpectrum right) {
        if (left == null) return right;
        if (right == null) return left;
        //folding 2 spectra with n and m non-monoisotopic peaks results in a new spectra with n+m non-monoisootopic peaks
        final int len = Math.min((left.size() + right.size()) - 1, maximalNumberOfPeaks);
        final double[] mz = new double[len];
        final double[] intensities = new double[len];

        for (int i = 0; i < Math.min(len, left.size()); ++i) {
            final double intensityLeft = left.getIntensityAt(i);
            final double mzLeft = left.getMzAt(i);
            for (int j = 0; j < Math.min(right.size(), len - i); ++j) {
                final double intensityRight = right.getIntensityAt(j);
                final double mzRight = right.getMzAt(j);
                final double folded = (intensityLeft * intensityRight);
                mz[i + j] += (mzLeft + mzRight) * folded;
                intensities[i + j] += folded;
            }
        }
        for (int k = 0; k < intensities.length; ++k) if (intensities[k] > 0) mz[k] /= intensities[k];
        return new ArrayWrapperSpectrum(mz, intensities);
    }
}
