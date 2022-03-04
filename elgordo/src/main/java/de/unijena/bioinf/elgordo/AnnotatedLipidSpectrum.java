/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class AnnotatedLipidSpectrum<T extends Spectrum<Peak>> implements Comparable<AnnotatedLipidSpectrum<T>>{

    private final static Set<MolecularFormula> BORING = new HashSet<>(Arrays.asList(
       MolecularFormula.parseOrThrow("H2O"),
            MolecularFormula.parseOrThrow("")
    ));
    private final static Set<MolecularFormula> ALMOST_BORING = new HashSet<>(Arrays.asList(
            MolecularFormula.parseOrThrow("H2O"),MolecularFormula.parseOrThrow("H4O2"),
            MolecularFormula.parseOrThrow("NH3"),MolecularFormula.parseOrThrow("OH"),
            MolecularFormula.parseOrThrow("H3O2"),
            MolecularFormula.parseOrThrow("")
    ));

    private final static HashMap<LipidClass, float[]> ModelsGeneric, ModelsSpecies;
    static {
        ModelsGeneric = new HashMap<>();
        ModelsSpecies = new HashMap<>();
        try {
            String[][] tableGeneric = FileUtils.readTable(FileUtils.ensureBuffering(new InputStreamReader(AnnotatedLipidSpectrum.class.getResourceAsStream("/elgordo/models.csv"))));
            for (int i=1; i < tableGeneric.length; ++i) {
                float[] coefs = new float[tableGeneric[i].length-1];
                for (int j=0; j < coefs.length; ++j) coefs[j] = Float.parseFloat(tableGeneric[i][j+1]);
                ModelsGeneric.put(LipidClass.valueOf(tableGeneric[i][0]),coefs);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(AnnotatedLipidSpectrum.class).error("Cannot parse elgordo model. Lipid detection is disabled.");
        }
        try {
            String[][] tableSpecies = FileUtils.readTable(FileUtils.ensureBuffering(new InputStreamReader(AnnotatedLipidSpectrum.class.getResourceAsStream("/elgordo/models_species.csv"))));
            for (int i=1; i < tableSpecies.length; ++i) {
                float[] coefs = new float[tableSpecies[i].length-1];
                for (int j=0; j < coefs.length; ++j) coefs[j] = Float.parseFloat(tableSpecies[i][j+1]);
                ModelsSpecies.put(LipidClass.valueOf(tableSpecies[i][0]),coefs);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(AnnotatedLipidSpectrum.class).error("Cannot parse elgordo model. Lipid detection is disabled.");
        }

    }

    private final T spectrum;
    private final MolecularFormula formula;
    private final double precursorMz;
    private final PrecursorIonType ionType;
    private final LipidSpecies annotatedSpecies;
    private final LipidAnnotation[][] annotationsPerPeak;
    private final int[] peakIndizes;
    private final double contradictionScore;

    private final int[] contradictingIndizes;
    private final LipidAnnotation[] contradictingAnnotations;

    private final LipidDetectionLevel detectionLevel;
    private final float formulaCorrectScore, classCorrectScore;

    private final int numberOfNondecomposableIntensivePeaks;
    private final float intensityOfNondecomposablePeaks;
    private final float lipidChainScore;

    public AnnotatedLipidSpectrum(T spectrum, MolecularFormula formula, double precursorMz, PrecursorIonType ionType, LipidSpecies species, float lipidChainScore, LipidAnnotation[][] annotationsPerPeak, int[] indizes, LipidAnnotation[] contradictingAnnotations, int[] contradictingIndizes, double contradictionScore, int numberOfNondecomposableIntensivePeaks, float intensityOfNondecomposablePeaks) {
        this.spectrum = spectrum;
        this.formula = formula;
        this.lipidChainScore = lipidChainScore;
        this.precursorMz = precursorMz;
        this.ionType = ionType;
        this.annotationsPerPeak = annotationsPerPeak;
        this.peakIndizes = indizes;
        this.annotatedSpecies = species;
        this.contradictingAnnotations = contradictingAnnotations;
        this.contradictingIndizes = contradictingIndizes;
        this.contradictionScore = contradictionScore;
        this.numberOfNondecomposableIntensivePeaks = numberOfNondecomposableIntensivePeaks;
        this.intensityOfNondecomposablePeaks = intensityOfNondecomposablePeaks;

        final float[] vector = makeFeatureVector();
        this.formulaCorrectScore = hasAnyPeakAnnotation() ? calcScore(vector, ModelsGeneric.get(this.annotatedSpecies.getLipidClass())) : 0f;
        this.classCorrectScore = isSpecified() ? calcScore(vector, ModelsSpecies.get(this.annotatedSpecies.getLipidClass())) : 0f;
        this.detectionLevel = classCorrectScore > 0 ? LipidDetectionLevel.CLASS_CORRECT : (formulaCorrectScore > 0 ? LipidDetectionLevel.FORMULA_CORRECT : LipidDetectionLevel.UNSPECIFIED);

    }

    private boolean hasAnyPeakAnnotation() {
        for (LipidAnnotation[] ano : annotationsPerPeak) {
            for (LipidAnnotation a : ano) {
                if (!(a instanceof PrecursorAnnotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private float calcScore(float[] vector, float[] coeffs) {
        if (coeffs==null) return 0f;
        float v = coeffs[vector.length];
        for (int i=0; i < vector.length; ++i) {
            v += vector[i]*coeffs[i];
        }
        return v;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public boolean isSpecified() {
        return !annotatedSpecies.chainsUnknown();
    }

    public boolean isPhosphocholin() {
        switch (annotatedSpecies.getLipidClass()) {
            case MG:
            case DG:
            case TG:
            case DGTS:
            case LDGTS:
            case MGDG:
            case DGDG:
            case SQDG:
            case SQMG:
                return false;
            case PC:
            case LPC:
            case PE:
            case LPE:
            case PS:
            case LPS:
            case PG:
            case LPG:
            case PI:
            case LPI:
            case PA:
            case LPA:
                return true;
            default: return false;
        }
    }

    private final static MolecularFormula[] steroidLosses = Arrays.stream(new String[]{
            "C8H18O3",
            "C2H4O2",
            "C8H16O2",
            "C7H14O2",
            "C6H14O2"
    }).map(MolecularFormula::parseOrThrow).toArray(MolecularFormula[]::new);
    private final static MolecularFormula[] steroidFragments = Arrays.stream(new String[]{
            "C19H24",
            "C16H16O",
            "C13H12O",
            "C12H10O",
            "C10H8O",
            "C19H26O",
            "C14H10",
            "C13H8",
            "C12H8",
            "C12H7",
            "C5H4O2",
            "C4H4O2",
            "C9H7",
            "C5H6O",
            "C6H4",
            "C19H26",
            "C12H12O",
            "C17H24"
    }).map(MolecularFormula::parseOrThrow).toArray(MolecularFormula[]::new);
    private float numberOfcommonSteroidFragments() {
        final Deviation dev = new Deviation(20);
        PrecursorIonType hplus = PrecursorIonType.getPrecursorIonType("[M+H]+");
        final double threshold = Spectrums.getMaximalIntensity(spectrum)*0.01;
        int count=0;
        for (MolecularFormula f : steroidFragments) {
            final double mass = hplus.addIonAndAdduct(f.getMass());
            final int i = Spectrums.mostIntensivePeakWithin(spectrum, mass, dev);
            if (i >=0 && spectrum.getIntensityAt(i) >= threshold) {
                ++count;
            }
        }
        return count;
    }
    private float numberOfcommonSteroidLosses() {
        final Deviation dev = new Deviation(20);
        final double threshold = Spectrums.getMaximalIntensity(spectrum)*0.01;
        // check different adducts
        double[] modifmass = new double[]{
                0d, PrecursorIonType.getPrecursorIonType("[M+H]+").getModificationMass() - PrecursorIonType.getPrecursorIonType("[M+Na]+").getModificationMass(),
                PrecursorIonType.getPrecursorIonType("[M+H]+").getModificationMass() - PrecursorIonType.getPrecursorIonType("[M+NH3+H]+").getModificationMass(),
        };
        int bestCount=0;
        for (double m : modifmass) {
            int count = 0;
            for (MolecularFormula f : steroidFragments) {
                final double mass = precursorMz - f.getMass() + m;
                final int i = Spectrums.mostIntensivePeakWithin(spectrum, mass, dev);
                if (i >= 0 && spectrum.getIntensityAt(i) >= threshold) {
                    ++count;
                }
            }
            if (count>bestCount) bestCount=count;
        }
        return bestCount;
    }

    private final static double waterMass = MolecularFormula.parseOrThrow("H2O").getMass();
    private float numerOfWaterLosses() {
        final Deviation dev = new Deviation(20);
        final double threshold = Spectrums.getMaximalIntensity(spectrum)*0.005;
        int count=0;
        for (int k=2; k < 4; ++k) {
            final double mass = this.precursorMz - k*waterMass;
            final int i = Spectrums.mostIntensivePeakWithin(spectrum, mass, dev);
            if (i >=0 && spectrum.getIntensityAt(i) >= threshold) {
                ++count;
            }
        }
        return count;
    }

    private float numberOfUnexplainedPeaksWithinTop(int k) {
        final List<Peak> peaks = Spectrums.extractPeakList(spectrum);
        peaks.sort(Comparator.comparingDouble(Peak::getIntensity).reversed());
        final double threshold = peaks.get(0).getIntensity()*0.01;
        int count=0;
        eachPeak:
        for (int i=0; i < Math.min(k, peaks.size()); ++i) {
            final double mz = peaks.get(i).getMass();
            if (peaks.get(i).getIntensity() < threshold) break;
            for (int index : peakIndizes) {
                if (index >= 0 && Math.abs(spectrum.getMzAt(index)-mz) < 0.01) {
                    continue eachPeak;
                }
            }
            ++count;
        }
        return count;
    }

    public int numberOfHeadGroupAnnotations() {
        return (int)Arrays.stream(annotationsPerPeak).filter(x-> Arrays.stream(x).anyMatch(y->y instanceof HeadGroupFragmentAnnotation)).count();
    }
    public int numberOfSpecificHeadGroupAnnotations() {
        return (int)Arrays.stream(annotationsPerPeak).filter(x->Arrays.stream(x).anyMatch(y -> y instanceof HeadGroupFragmentAnnotation
        && !BORING.contains(y.getUnderlyingFormula()))).count();
    }
    public int numberOfChainAnnotations() {
        return (int)Arrays.stream(annotationsPerPeak).filter(x->Arrays.stream(x).anyMatch(y -> y instanceof ChainAnnotation)).count();
    }

    public int numberOfAnnotatedPeaks() {
        int count=0;
        int prev=-1;
        for (int p : peakIndizes) {
            if (p!=prev) {
                ++count;
                prev = p;
            }
        }
        return count;
    }
    public int numberOfUnexplainedIntensivePeaks(double threshold) {
        final double mx = Spectrums.getMaximalIntensity(spectrum)*threshold;
        int count=0;
        int j=0;
        for (int k=0; k < spectrum.size(); ++k) {
            while (j < peakIndizes.length && k>peakIndizes[j]) {
                ++j;
            }
            if ((j >= peakIndizes.length || k!=peakIndizes[j]) && spectrum.getIntensityAt(k)>mx) {
                ++count;
            }
        }
        return count;
    }

    public LipidAnnotation getAnnotationAt(int k) {
        return annotationsPerPeak[k][0];
    }

    public int getPeakIndexAt(int k) {
        return peakIndizes[k];
    }

    public T getSpectrum() {
        return spectrum;
    }

    public LipidSpecies getAnnotatedSpecies() {
        return annotatedSpecies;
    }

    private static final MolecularFormula H2O = MolecularFormula.parseOrThrow("H2O"), NH3 = MolecularFormula.parseOrThrow("NH3");

    // explained intensity of spectrum. Ignore "trivial" peaks: precursor, H2O, and NH3
    public double explainedIntensityOfNontrivialPeaks() {
        double precursorMz = -1;
        double explained = 0d;
        double trivialIntensity = 0d;
        int dup=-1;
        int j=0;
        for (int peak : peakIndizes) {
            if (peak==dup) {
            } else if (peak >= 0) {
                dup=peak;
                final LipidAnnotation a = annotationsPerPeak[j][0];
                if (a instanceof PrecursorAnnotation) {
                    precursorMz = spectrum.getMzAt(peak);
                } else {
                    if (a.getTarget()== LipidAnnotation.Target.LOSS) {
                        if (a.getUnderlyingFormula().isEmpty() || a.getUnderlyingFormula().equals(H2O) || a.getUnderlyingFormula().equals(NH3)) {
                            trivialIntensity += spectrum.getIntensityAt(peak);
                        }
                    }
                    explained += spectrum.getIntensityAt(peak);
                }
            }
            ++j;
        }
        double all = 0d;
        for (int k=0; k < spectrum.size(); ++k) {
            if (precursorMz>0 && Math.abs(spectrum.getMzAt(k)-precursorMz) < 0.3) {
                // do not include precursor intensity
            } else all += spectrum.getIntensityAt(k);;
        }
        explained -= trivialIntensity;
        all -= trivialIntensity;
        if (all <=0) return 0d;
        return explained / all;
    }

    public double getContradictionScore() {
        return contradictionScore;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s, %d peaks annotated (%.2f %% intensity)", annotatedSpecies.toString(), numberOfAnnotatedPeaks(), explainedIntensityOfNontrivialPeaks()*100d);
    }

    @Override
    public int compareTo(@NotNull AnnotatedLipidSpectrum<T> o) {
        int c = Double.compare(explainedIntensityOfNontrivialPeaks(), o.explainedIntensityOfNontrivialPeaks());
        if (c==0) {
            c = Integer.compare(isSpecified() ? 1 : -1, o.isSpecified() ? 1 : -1);
        }
        if (c==0) {
            c = Integer.compare(peakIndizes.length, o.peakIndizes.length);
        }
        if (c==0) {
            if (this.ionType.isPlainProtonationOrDeprotonation() && !o.ionType.isPlainProtonationOrDeprotonation()) {
                return 1;
            } else if (o.ionType.isPlainProtonationOrDeprotonation() && !this.ionType.isPlainProtonationOrDeprotonation()) {
                return -1;
            }
        }
        return c;
    }

    public int numberOfContradictingAnnotations() {
        return contradictingIndizes.length;
    }

    public LipidAnnotation getContradictingAnnotation(int i) {
        return contradictingAnnotations[i];
    }
    public int getContradictingPeakIndex(int i) {
        return contradictingIndizes[i];
    }

    public LipidDetectionLevel getDetectionLevel() {
        return detectionLevel;
    }

    public float getFormulaCorrectScore() {
        return formulaCorrectScore;
    }

    public float getClassCorrectScore() {
        return classCorrectScore;
    }


    public float[] makeFeatureVector() {
        float intensitySum = 0f;
        for (int i=0; i < spectrum.size(); ++i) intensitySum += spectrum.getIntensityAt(i);
        int uncommonSmallChainLength = 0, uncommonLargeChainLength = 0, strangeDifference = 0;
        final boolean phosphocholin = isPhosphocholin();
        int numberOfAlkylChains = (int)Arrays.stream(getAnnotatedSpecies().getChains()).filter(x->x.getType()== LipidChain.Type.ALKYL).count(), maxDoubleBonds = 0;
        {
            final IntSummaryStatistics chainLengths = Arrays.stream(annotatedSpecies.getChains()).mapToInt(x -> x.chainLength).summaryStatistics();
            maxDoubleBonds = Arrays.stream(annotatedSpecies.getChains()).mapToInt(x->x.numberOfDoubleBonds).max().orElse(0);
            if (phosphocholin) {
                // everything below 6 is uncommon:
                uncommonSmallChainLength = Math.max(0, 6 - chainLengths.getMin());
                // everything above 36 is uncommon
                uncommonLargeChainLength = Math.max(0, chainLengths.getMax() - 36);
                // might be imbalanced
                strangeDifference = 0;
            } else {
                // everything below 8 is uncommon:
                uncommonSmallChainLength = Math.max(0, 8 - chainLengths.getMin());
                // everything above 22 is uncommon
                uncommonLargeChainLength = Math.max(0, chainLengths.getMax() - 22);
                // each chain should be more than halve the largest chain
                strangeDifference = Math.max(0,chainLengths.getMax()/2 - chainLengths.getMin());
            }
        }

        final int nspec = numberOfSpecificHeadGroupAnnotations();
        return new float[]{
                (float)explainedIntensityOfNontrivialPeaks(),
                nspec,
                Math.min(18, numberOfChainAnnotations()),
                //Math.min(50, uncommonLargeChainLength),
                uncommonSmallChainLength,
                Math.min(30, strangeDifference),
                0f,
                numberOfAlkylChains,
                Math.min(35, Math.max(0, maxDoubleBonds - 6)),
                Math.min(30, numberOfUnexplainedIntensivePeaks(0.01)),
                // add classificators to distinguish lipids from steroids
                numerOfWaterLosses(),
                numberOfcommonSteroidFragments(),
                numberOfcommonSteroidLosses(),
                (float)getContradictionScore(),
                intensitySum==0 ? 0f : (float)Arrays.stream(contradictingIndizes).mapToDouble(spectrum::getIntensityAt).sum()/intensitySum,
                isSpecified() ? 1 : -1,
                // additional features
                isSpecified() ? nspec : 0,
                (float)(Math.log(explainedIntensityOfNontrivialPeaks()+1e-4)-Math.log(1e-4)),
                nspec * Math.min(18, numberOfChainAnnotations()),

                numberOfNondecomposableIntensivePeaks,
                intensityOfNondecomposablePeaks,
                ratioOfSpecificFragments(),
                this.lipidChainScore

        };
    }

    private float ratioOfSpecificFragments() {
        final FragmentLib.FragmentSet fragmentSet = this.annotatedSpecies.getLipidClass().fragmentLib.getFor(this.ionType).get();
        int shouldHave = 0;
        for (MolecularFormula f : fragmentSet.losses) {
            if (!ALMOST_BORING.contains(f)) ++shouldHave;
        }
        for (MolecularFormula f : fragmentSet.fragments) {
            ++shouldHave;
        }
        if(shouldHave==0) return 0f;


        final int have = (int)Arrays.stream(annotationsPerPeak).filter(x->Arrays.stream(x).anyMatch(y -> y instanceof HeadGroupFragmentAnnotation
                && !ALMOST_BORING.contains(y.getUnderlyingFormula()))).count();
        return (float)((double)have / (double)shouldHave);
    }


}
