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

package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.Random;

public class FragmentIsotopeGenerator extends FastIsotopePatternGenerator {

    public static void main(String[] args) {
        final FastIsotopePatternGenerator isoGen = new FastIsotopePatternGenerator();
        isoGen.setMinimalProbabilityThreshold(0);
        isoGen.setMaximalNumberOfPeaks(5);
        final Random random = new Random(12345);
        for (int k=0; k < 50000; ++k) {
            final int C = random.nextInt(100)+10;
            final int H = random.nextInt(50)+20;
            final int N = random.nextInt(20);
            final int O = Math.max(random.nextInt(30)-10,0);
            final int S = Math.max(random.nextInt(50)-30,0);
            final int Br = Math.max(random.nextInt(100-90),0);
            final int Cl = Math.max(random.nextInt(100-90),0);
            final int I = Math.max(random.nextInt(100-90),0);
            MolecularFormula f = MolecularFormula.emptyFormula();
            f=f.add(MolecularFormula.parse("C").multiply(C));
            f=f.add(MolecularFormula.parse("H").multiply(H));
            f=f.add(MolecularFormula.parse("N").multiply(N));
            f=f.add(MolecularFormula.parse("O").multiply(O));
            f=f.add(MolecularFormula.parse("S").multiply(S));
            f=f.add(MolecularFormula.parse("Br").multiply(Br));
            f=f.add(MolecularFormula.parse("Cl").multiply(Cl));
            f=f.add(MolecularFormula.parse("I").multiply(I));

            MolecularFormula loss = MolecularFormula.emptyFormula();
            if (C>0)loss=loss.add(MolecularFormula.parse("C").multiply(random.nextInt(C)));
            if (H>0)loss=loss.add(MolecularFormula.parse("H").multiply(random.nextInt(H)));
            if (N>0)loss=loss.add(MolecularFormula.parse("N").multiply(random.nextInt(N)));
            if (O>0)loss=loss.add(MolecularFormula.parse("O").multiply(random.nextInt(O)));
            if (S>0)loss=loss.add(MolecularFormula.parse("S").multiply(random.nextInt(S)));
            if (Br>0)loss=loss.add(MolecularFormula.parse("Br").multiply(random.nextInt(Br)));
            if (Cl>0)loss=loss.add(MolecularFormula.parse("Cl").multiply(random.nextInt(Cl)));
            if (I>0)loss=loss.add(MolecularFormula.parse("I").multiply(random.nextInt(I)));

            final MolecularFormula fragment = f.subtract(loss);

            System.out.println(f.toString() + "\t" + fragment.toString());

            final SimpleSpectrum ms1 = isoGen.simulatePattern(f, PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization());

            final SimpleSpectrum ms2New = Spectrums.getNormalizedSpectrum(new FragmentIsotopeGenerator().simulatePattern(ms1, f, loss, PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization()), Normalization.Sum(1d));

            final SimpleSpectrum ms2Old = isoGen.simulatePattern(fragment, PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization());

            for (int i=0; i < ms2New.size(); ++i) {
                if (Math.abs(ms2New.getMzAt(i)-ms2Old.getMzAt(i)) > 1e-6) {
                    throw new RuntimeException("m/z error: " +  ms2New.getMzAt(i) + " vs. " + ms2Old.getMzAt(i) + " for " + i + "th peak in " + f + " with fragment " + fragment);
                }
                if (Math.abs(ms2New.getIntensityAt(i)-ms2Old.getIntensityAt(i)) > 1e-6) {
                    throw new RuntimeException("int error: " +  ms2New.getIntensityAt(i) + " vs. " + ms2Old.getIntensityAt(i) + " for " + f + " with fragment " + fragment);
                }
            }


        }
    }

    public SimpleSpectrum simulatePattern(Spectrum<Peak> ms1, MolecularFormula parent, MolecularFormula loss, Ionization ionization) {
        final SimpleMutableSpectrum parentSpectrum = new SimpleMutableSpectrum(ms1.size());
        for (int k=0; k < ms1.size(); ++k) {
            parentSpectrum.addPeak(ionization.subtractFromMass(ms1.getMzAt(k)), ms1.getIntensityAt(k));
        }

        final int mono=parent.getIntMass();
        for (int k=0; k < parentSpectrum.size(); ++k) {
            parentSpectrum.setMzAt(k, parentSpectrum.getMzAt(k)-mono - k);
        }


        Spectrums.normalize(parentSpectrum, Normalization.Sum(1));
        final SimpleMutableSpectrum lossSpectrum = foldFormula(loss, parentSpectrum.size(), 0d);
        while (lossSpectrum.size() < parentSpectrum.size()) lossSpectrum.addPeak(0d, 0d);

        final double[] intensities = new double[parentSpectrum.size()];
        final double[] masses = new double[parentSpectrum.size()];
        for (int k=0; k < intensities.length; ++k) {
            intensities[k] = parentSpectrum.getIntensityAt(k);
            masses[k] = parentSpectrum.getMzAt(k)*parentSpectrum.getIntensityAt(k);
            for (int j=k; j > 0; --j) {
                final double folded = lossSpectrum.getIntensityAt(j)*intensities[k-j];
                intensities[k] -= folded;
                masses[k] -= (lossSpectrum.getMzAt(j)+masses[k-j])*folded;
            }
            intensities[k] /= lossSpectrum.getIntensityAt(0);
            masses[k] -= lossSpectrum.getMzAt(0)*intensities[k]*lossSpectrum.getIntensityAt(0);
            masses[k] /= (intensities[k]*lossSpectrum.getIntensityAt(0));
        }

        final double monof = parent.getIntMass()-loss.getIntMass();
        for (int k=0; k < masses.length; ++k) {
            masses[k] = monof + k+ionization.addToMass(masses[k]);
        }

        return new SimpleSpectrum(masses, intensities);
    }

}
