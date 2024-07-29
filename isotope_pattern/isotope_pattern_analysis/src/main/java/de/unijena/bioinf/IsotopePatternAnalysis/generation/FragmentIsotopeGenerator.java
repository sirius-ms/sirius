

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
import gnu.trove.list.array.TDoubleArrayList;

public class FragmentIsotopeGenerator extends FastIsotopePatternGenerator {

    public static void main(String[] args) {
        /*
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
        */

        final double[] values = new double[]{
                309.0437, 127.27,
                310.038, 999.0,
                311.0415, 87.81,
        };
        final TDoubleArrayList mzvalues=new TDoubleArrayList(), intvalues = new TDoubleArrayList();
        for (int k=0; k < values.length; k+=2) {
            mzvalues.add(values[k]);
            intvalues.add(values[k+1]);
        }

        final Ionization H = PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization();

        final SimpleSpectrum spec = new SimpleSpectrum(mzvalues.toArray(),intvalues.toArray());
                //new SimpleSpectrum(new double[]{269.1108,270.1138,271.106}, new double[]{999.0,205.39,2.4});
        final MolecularFormula A = MolecularFormula.parseOrThrow("C13H14BrN3O");
        final MolecularFormula B = MolecularFormula.parseOrThrow("C5H7N");

        final double MAX = 999;

        final FragmentIsotopeGenerator fiso = new FragmentIsotopeGenerator();
        final SimpleSpectrum a = Spectrums.getNormalizedSpectrum(fiso.simulatePattern(spec, A, A.subtract(B), PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization()), Normalization.Max(MAX));


        final SimpleSpectrum right = Spectrums.getNormalizedSpectrum(fiso.simulateFragmentPatternWithImperfectFilter(spec, B, A.subtract(B), PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization()), Normalization.Max(MAX));
        System.out.println(right);
        System.out.println(a);
        System.out.println(Spectrums.getNormalizedSpectrum(new FastIsotopePatternGenerator().simulatePattern(B,PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization()), Normalization.Max(MAX)));


    }

    public SimpleSpectrum simulatePattern(Spectrum<Peak> ms1, MolecularFormula parent, MolecularFormula loss, Ionization ionization) {
        return simulatePattern(ms1,parent,loss,ionization,false);
    }

    /**
     * Simulates the isotope pattern of the fragment while considering the bias in the MS1 isotope pattern
     * @param ms1 ms1 pattern
     * @param parent molecular formula of parent
     * @param loss the loss between the ion formula and the fragment formula
     * @param ionization ionization of the peaks
     * @param simulateExactMasses if false, consider also mass biases. This might make sense if MS1 and MS/MS have the same mass shift bias
     * @return
     */
    public SimpleSpectrum simulatePattern(Spectrum<Peak> ms1, MolecularFormula parent, MolecularFormula loss, Ionization ionization, boolean simulateExactMasses) {
        final SimpleMutableSpectrum parentSpectrum = new SimpleMutableSpectrum(ms1.size());
        for (int k=0; k < ms1.size(); ++k) {
            parentSpectrum.addPeak(ionization.subtractFromMass(ms1.getMzAt(k)), ms1.getIntensityAt(k));
        }

        if (simulateExactMasses) {
            final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(distribution, mode);
            gen.setMaximalNumberOfPeaks(ms1.size());
            gen.setMinimalProbabilityThreshold(0d);
            final SimpleSpectrum exactMasses = gen.simulatePattern(parent,ionization);
            final int mono=parent.getIntMass();
            for (int k=0; k < parentSpectrum.size(); ++k) {
                parentSpectrum.setMzAt(k, ionization.subtractFromMass(exactMasses.getMzAt(k))-mono - k);
            }
        } else {
            final int mono=parent.getIntMass();
            for (int k=0; k < parentSpectrum.size(); ++k) {
                parentSpectrum.setMzAt(k, parentSpectrum.getMzAt(k)-mono - k);
            }
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


        //may have negative intensities after folding
        boolean someNegative = false;
        for (double intensity : intensities) {
            if (intensity<0) {
                someNegative = true;
                break;
            }
        }
        if (!someNegative) return new SimpleSpectrum(masses, intensities);

        final TDoubleArrayList mzs = new TDoubleArrayList(intensities.length);
        final TDoubleArrayList ints = new TDoubleArrayList(intensities.length);
        for (int i = 0; i < intensities.length; i++) {
            if (intensities[i]>=getMinimalProbabilityThreshold()){
                ints.add(intensities[i]);
                mzs.add(masses[i]);
            }
        }
        return new SimpleSpectrum(mzs.toArray(), ints.toArray());
    }


    public SimpleSpectrum simulateFragmentPatternWithImperfectFilter(SimpleSpectrum parentPattern, MolecularFormula fragment, MolecularFormula loss, Ionization ion) {
        final FastIsotopePatternGenerator generator = new FastIsotopePatternGenerator(Normalization.Sum(1d));
        final int n = parentPattern.size();
        generator.setMaximalNumberOfPeaks(n);
        generator.setMinimalProbabilityThreshold(0d);

        parentPattern = Spectrums.getNormalizedSpectrum(parentPattern, Normalization.Sum(1d));
        final SimpleSpectrum fragmentPattern = generator.simulatePattern(fragment, ion);
        final SimpleSpectrum lossPattern = generator.simulatePattern(loss, ion);

        final double[][] matrix = new double[n][n];
        for (int i=0; i < Math.min(n,fragmentPattern.size()); ++i) {
            for (int j=0; j < Math.min(n,lossPattern.size()); ++j) {
                matrix[i][j] = fragmentPattern.getIntensityAt(i)*lossPattern.getIntensityAt(j);
            }
        }
        // learn imperfect filter
        for (int k=0; k < n; ++k) {
            double sum = 0d;
            for (int i=0; i <= k; ++i) {
                final int j = k-i;
                sum += matrix[i][j];
            }
            double multiplicator = parentPattern.getIntensityAt(k)/sum;
            for (int i=0; i <= k; ++i) {
                final int j = k-i;
                matrix[i][j] *= multiplicator;
            }
        }
        // fix fragment pattern
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(fragmentPattern.size());
        for (int i=0; i < Math.min(fragmentPattern.size(), n); ++i) {
            double intensity = 0d;
            for (int j=0; j < n; ++j) {
                intensity += matrix[i][j];
            }
            buf.addPeak(fragmentPattern.getMzAt(i), intensity);
        }

        return new SimpleSpectrum(buf);


    }




















}
