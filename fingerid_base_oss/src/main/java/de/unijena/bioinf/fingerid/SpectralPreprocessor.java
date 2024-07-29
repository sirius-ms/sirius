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
package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.Comparator;

public class SpectralPreprocessor {
    private Sirius analyzer;

    private final boolean modifySpectrum;

    public SpectralPreprocessor() {
        this(new Sirius());
    }

    public SpectralPreprocessor(Sirius analyzer) {
        this(analyzer,true);
    }

    public SpectralPreprocessor(Sirius analyzer, boolean modifySpectrum) {
        this.analyzer = analyzer;
        this.modifySpectrum = modifySpectrum;
    }


    public static InputFeatures preprocessFromSirius(Sirius sirius, IdentificationResult result, Ms2Experiment experiment) {
        return new SpectralPreprocessor(sirius).extractInputFeatures(result.getTree(), experiment);
    }

    public InputFeatures extractInputFeatures(FTree tree, Ms2Experiment spectralInput) {
        final MutableMs2Experiment experiment = new MutableMs2Experiment(spectralInput);

        // avoid that we delete all the peaks in the spectrum!
        experiment.setAnnotation(NoiseThresholdSettings.class, new NoiseThresholdSettings(0.0001, Integer.MAX_VALUE, NoiseThresholdSettings.BASE_PEAK.LARGEST, 0));

        // cleanup spectrum
        final ProcessedInput input = analyzer.preprocessForMs2Analysis(experiment);
        final SpectralRecalibration recalibration = tree.getAnnotation(SpectralRecalibration.class, SpectralRecalibration::none);


        // first preprocess tree
        tree = new IonTreeUtils().treeToNeutralTree(tree);

        //get iontype and formula from tree
        final PrecursorIonType precursorIonType = tree.getAnnotationOrThrow(PrecursorIonType.class);
        final MolecularFormula formula = tree.getRoot().getFormula();

        final FragmentAnnotation<AnnotatedPeak> anoPeak = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        final FragmentAnnotation<Peak> anoPeakLegacy = tree.getOrCreateFragmentAnnotation(Peak.class);


        double minInt = Double.POSITIVE_INFINITY;
        for (Fragment f : tree) {
            AnnotatedPeak pk = anoPeak.get(f);
            if (pk.isMeasured()) {
                minInt = Math.min(minInt, pk.getRelativeIntensity());
            }
        }

        // allow only peaks explaining a subset of parent formula PLUS a single nitrogen and a single water
        final MolecularFormula parentFormula = formula.add(MolecularFormula.parseOrThrow("H2ON"));
        final MassToFormulaDecomposer decomposer = analyzer.getMs2Analyzer().getDecomposerFor(new ChemicalAlphabet(parentFormula.elementArray()));
        final FormulaConstraints constraints = FormulaConstraints.allSubsetsOf(parentFormula);

        // first delete all isotope peaks from spectrum
        final Deviation dev = new Deviation(10);
        input.getMergedPeaks().sort(Comparator.comparingDouble(Peak::getMass));
        FragmentAnnotation<Ms2IsotopePattern> isoPat = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        if (modifySpectrum && isoPat!=null) {
            TIntHashSet toDelete = new TIntHashSet();
            final Spectrum<ProcessedPeak> spec = Spectrums.wrap(input.getMergedPeaks());
            for (Fragment f : tree) {
                Ms2IsotopePattern ms2IsotopePattern = isoPat.get(f);
                if (ms2IsotopePattern!=null) {
                    for (int k=1; k < ms2IsotopePattern.getPeaks().length; ++k) {
                        double mz = ms2IsotopePattern.getPeaks()[k].getMass();
                        int pos = Spectrums.indexOfFirstPeakWithin(spec, mz, dev);
                        if (pos<0) {
                            continue;
                        }
                        final double threshold = mz+dev.absoluteFor(mz);
                        for (; pos < spec.size(); ++pos) {
                            if (spec.getMzAt(pos) > threshold) {
                                break;
                            }
                            toDelete.add(pos);
                        }
                    }
                }
            }
            final int[] del = toDelete.toArray();
            Arrays.sort(del);
            for (int k=del.length-1; k >= 0; --k) {
                input.getMergedPeaks().remove(del[k]);
            }
        }


        // recalibrate each peak
        final IonMode ion = (IonMode) precursorIonType.getIonization();
        // always also allow protonation/deprotonation
        IonMode proton = (IonMode) (precursorIonType.getCharge() > 0 ? PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization() : PrecursorIonType.getPrecursorIonType("[M-H]-").getIonization());
        if (ion.equals(proton)) proton=null;
        final SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(input.getMergedPeaks().size());
        if (modifySpectrum) {
            final double intensityThreshold = Math.min(0.01, minInt/4d);
            for (ProcessedPeak pk : input.getMergedPeaks()) {
                if (pk.isSynthetic() && pk==input.getParentPeak())
                    continue; // we do that later
                if (pk.getIntensity() < intensityThreshold) {
                    continue;
                }
                if (pk.getIntensity() < 0.05 && !decomposer.formulaIterator(pk.getMass(), ion, dev, constraints).hasNext() && (proton==null || !decomposer.formulaIterator(pk.getMass(), proton, dev, constraints).hasNext())) {
                    continue; // not a single decomposition...
                }
                final double recalibratedMass = recalibration.recalibrate(pk);
                mutableSpectrum.addPeak(recalibratedMass, pk.getRelativeIntensity());
            }
        } else {
            for (ProcessedPeak  pk : input.getMergedPeaks()) mutableSpectrum.addPeak(pk);
        }
        // renormalize spectrum
        Spectrums.normalize(mutableSpectrum, Normalization.Max(1d));
        // now create new spectrum by removing precursor information
        final double exactParentMass = precursorIonType.neutralMassToPrecursorMass(formula.getMass());
        if (input.getParentPeak().isSynthetic()) {
            mutableSpectrum.addPeak(exactParentMass, 1d);
        }

        final SimpleMutableSpectrum copy = new SimpleMutableSpectrum(mutableSpectrum);
        if (precursorIonType.isPlainProtonationOrDeprotonation()) {
            Spectrums.addOffset(copy, - precursorIonType.getIonization().getMass(), 0d);
        } else {
            // for in-source fragmentation, we add artificial peaks
            double adduct = precursorIonType.getAdduct().getMass();
            double insource = precursorIonType.getInSourceFragmentation().getMass();
            if (adduct>0) {
                copy.addPeak(exactParentMass - adduct, 1d );
            }
            if (adduct>0) {
                copy.addPeak(exactParentMass + insource, 1d );
            }
            // probably it is safer to just subtract all Na+ mz from spectrum and match as it is. The proton peaks would
            // match by the raw spectrum anyways
            Spectrums.addOffset(copy, - precursorIonType.getIonization().getMass(), 0d);
        }

        for (Fragment f : tree) {
            final AnnotatedPeak pk = anoPeak.get(f, AnnotatedPeak::none);
            if (f.isRoot()) {
                anoPeakLegacy.set(f, new SimplePeak(exactParentMass, 1d));
            } else if (pk.isMeasured()) {
                anoPeakLegacy.set(f, new SimplePeak(pk.getRecalibratedMass(), pk.getRelativeIntensity()));
            } else {
                anoPeakLegacy.set(f, new SimplePeak(pk.getRecalibratedMass(), 0d));
            }
        }

        return new InputFeatures(tree, new SimpleSpectrum(mutableSpectrum), new SimpleSpectrum(copy), exactParentMass);
    }
}
