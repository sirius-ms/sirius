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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Removes possible isotope peaks from input. Is using the correct molecular formula of the ion
 */
public class IsotopePeakWithKnownParentFilter implements Preprocessor {

    private final static int MAX_NUM_ISO = 4;

    private final boolean verbose;

    public IsotopePeakWithKnownParentFilter() {
        this.verbose = false;
    }

    public IsotopePeakWithKnownParentFilter(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public MutableMs2Experiment process(MutableMs2Experiment experiment) {
        final PeriodicTable p = PeriodicTable.getInstance();
        final IsotopicDistribution dist = p.getDistribution();
        final Isotopes HIso = dist.getIsotopesFor(p.getByName("H"));
        final double[] minDists = new double[MAX_NUM_ISO];
        final double[] maxDists = new double[MAX_NUM_ISO];
        final double Hdiff = HIso.getMass(1)-HIso.getMass(0);
        for (int k=0; k < MAX_NUM_ISO; ++k) minDists[k] = maxDists[k] = Hdiff*(k+1);

        for (Element e : experiment.getMolecularFormula().elementArray()) {
            final Isotopes isotope = dist.getIsotopesFor(e);
            if (isotope != null && isotope.getNumberOfIsotopes()>1) {
                for (int k=1; k < Math.min(MAX_NUM_ISO+1, isotope.getNumberOfIsotopes()); ++k) {
                    final double distance = isotope.getMass(k)-isotope.getMass(0);
                    final int step = isotope.getIntegerMass(k)-isotope.getIntegerMass(0);
                    if (step<=0) throw new RuntimeException("Strange Isotope definition: +1 peak has same unit mass as +0 peak");
                    int repeats = 1;
                    for (int l=step; l <= MAX_NUM_ISO; l += step ) {
                        minDists[l-1] = Math.min(minDists[l-1], distance*repeats);
                        maxDists[l-1] = Math.max(maxDists[l-1], distance*repeats);
                        ++repeats;
                    }
                }
            }
        }

        final ChemicalAlphabet alphabet = new ChemicalAlphabet(experiment.getMolecularFormula().elementArray());
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(alphabet);
        final Deviation dev = experiment.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
        final Deviation shiftDev = dev.divide(2d);
        final FormulaConstraints fc = new FormulaConstraints(alphabet);
        final IsotopePatternGenerator generator = new FastIsotopePatternGenerator(Normalization.Sum(1d));
        experiment.getMolecularFormula().visit(new FormulaVisitor<Object>() {
            @Override
            public Object visit(Element element, int i) {
                fc.setUpperbound(element, i);
                return null;
            }
        });
        final List<MutableMs2Spectrum> newSpectras = new ArrayList<MutableMs2Spectrum>();
        final PrecursorIonType ionType = experiment.getPrecursorIonType();
        final TIntArrayList maybeIsotope = new TIntArrayList(5);

        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            int signals = 0;
            final SimpleMutableSpectrum sms = new SimpleMutableSpectrum(spec);
            final SimpleMutableSpectrum byInt = new SimpleMutableSpectrum(sms);
            final BitSet isotopeFlags = new BitSet(sms.size());
            final BitSet isotopeMono = new BitSet(sms.size());
            final double intensityThresholdForSignalPeaks = 500;//Math.max(byInt.getIntensityAt(Math.min(byInt.size()-1,80)), byInt.getIntensityAt(2)*0.005);
            Spectrums.sortSpectrumByDescendingIntensity(byInt);
            Spectrums.sortSpectrumByMass(sms);
            int numPattern = 0;
            for (int i=0; i < byInt.size(); ++i) {
                final Peak peak = byInt.getPeakAt(i);
                if (peak.getIntensity() < intensityThresholdForSignalPeaks) break;
                final int index = Spectrums.binarySearch(sms, peak.getMass());
                final List<MolecularFormula> formulas = decomposer.decomposeToFormulas(ionType.getIonization().subtractFromMass(peak.getMass()), dev,fc );
                boolean atLeastOne = false;
                if (formulas.size() > 0 && peak.getIntensity()>=500) ++signals;
                eachFormula:
                for (MolecularFormula formula : formulas) {
                    maybeIsotope.clear();
                    final SimpleSpectrum pattern = generator.simulatePattern(formula, ionType.getIonization());
                    int pki=1;
                    if (pki >= pattern.size()) continue eachFormula;
                    // now match this pattern to peaks

                    final SimpleMutableSpectrum BUF = new SimpleMutableSpectrum();
                    BUF.addPeak(sms.getPeakAt(index));

                    for (int k = index+1; k < sms.size(); ++k) {
                        // check intensity
                        final double relativeIntensity = sms.getIntensityAt(k)/sms.getIntensityAt(index);
                        final double thInt = pattern.getIntensityAt(pki)/pattern.getIntensityAt(0);
                        final double relDev = relativeIntensity/thInt;
                        final double absDiv = Math.abs(relativeIntensity-thInt);
                        final double absAbs = Math.abs((thInt*sms.getIntensityAt(index))-sms.getIntensityAt(k));
                        final boolean intensityMatch = ((relDev >= 0.7 && relDev <= 1.3) || absDiv < 0.05 || absAbs <= 500);
                        if ((shiftDev.inErrorWindow(pattern.getMzAt(pki)-pattern.getMzAt(0), sms.getMzAt(k)-sms.getMzAt(index)) || dev.inErrorWindow(pattern.getMzAt(pki), sms.getMzAt(k))) && intensityMatch) {
                            maybeIsotope.add(k);
                            BUF.addPeak(sms.getPeakAt(k));
                            if (++pki >= pattern.size()) break;
                        } else if (sms.getMzAt(k) - pattern.getMzAt(pki) > 0.2) {
                            // if isotope peak is missing and its intensity is above 10%, this pattern is not matching
                            if (pattern.getIntensityAt(pki) > 0.1) continue eachFormula;
                            else break;
                        }
                    }
                    if (pki>1 && (pki >= pattern.size() || pattern.getIntensityAt(pki) <= 0.1 ||
                            ((pattern.getIntensityAt(pki)/pattern.getIntensityAt(0))*sms.getIntensityAt(index)) <= 300)) {
                        // we found an match
                        if (verbose) {
                            System.out.println("Found isotope peak for " + formula + ":");
                            System.out.println(Spectrums.getNormalizedSpectrum(pattern, Normalization.Max(1d)));
                            System.out.println(BUF);
                            Spectrums.normalize(BUF, Normalization.Max(1d));
                            System.out.println(BUF);
                        }
                        atLeastOne = true;
                        for (int k : maybeIsotope.toArray()) isotopeFlags.set(k);
                        isotopeMono.set(index);
                    }
                }
                if (atLeastOne) ++numPattern;
            }
            int numIsoPeaks = 0;
            final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(sms.size());
            for (int k=0; k < sms.size(); ++k) {
                if (!isotopeFlags.get(k) || isotopeMono.get(k)) buffer.addPeak(sms.getMzAt(k), sms.getIntensityAt(k));
                else ++numIsoPeaks;
            }
            System.out.println("#pattern = " + numPattern +"\t#peaks = " + numIsoPeaks + "\t of " + sms.size() + " peaks\t" + signals + " signals");

            newSpectras.add(new MutableMs2Spectrum(buffer, spec.getPrecursorMz(), spec.getCollisionEnergy(), spec.getMsLevel()));
        }

        experiment.setMs2Spectra(newSpectras);
        return experiment;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
    }
}
