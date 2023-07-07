
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import org.apache.commons.math3.special.Erf;
import org.slf4j.LoggerFactory;

/**
 * @author Kai Dührkop
 */
@Called("Mass Deviation")
public class MassDeviationVertexScorer implements DecompositionScorer<MassDeviationVertexScorer.Prepared> {
    private final static double sqrt2 = Math.sqrt(2);

    private boolean useOriginalMz = false;

    protected double weight = 1d;

    public MassDeviationVertexScorer() {
    }

    @Override
    public Prepared prepare(ProcessedInput input) {
        final Ms1IsotopePattern ms1 = input.getAnnotation(Ms1IsotopePattern.class, Ms1IsotopePattern::none);
        int k = Spectrums.mostIntensivePeakWithin(ms1.getSpectrum(), input.getParentPeak().getMass(), input.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation);
        SpectralRecalibration rec = input.getAnnotation(SpectralRecalibration.class, SpectralRecalibration::none);
        if (k >= 0)
            return new Prepared(rec, ms1, ms1.getSpectrum().getPeakAt(k));
        else return new Prepared(rec,null,null);
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion,ProcessedPeak peak, ProcessedInput input, Prepared prep) {
        if (peak.getOriginalPeaks().isEmpty())
            return 0d; // don't score synthetic peaks
        if (peak==input.getParentPeak() && prep.parentPeakFromMs1!=null) {
            // score parent peak with MS1 instead
            final double realMass = prep.parentPeakFromMs1.getMass();
            final Deviation dev = input.getAnnotationOrDefault(MS1MassDeviation.class).standardMassDeviation;
            return score(formula, ion, realMass, dev);
        } else {
            final double realMass = useOriginalMz ? peak.getMass() : prep.recalibration.recalibrate(peak);
            final Deviation dev = input.getExperimentInformation().getAnnotationOrDefault(MS2MassDeviation.class).standardMassDeviation;
            return score(formula,ion,realMass,dev);
        }
    }

    public double score(MolecularFormula formula, Ionization ion, double realMass, Deviation dev) {
        final double theoreticalMass = ion.addToMass(formula.getMass());
        final double sd = dev.absoluteFor(realMass);
        double score = weight * Math.log(Erf.erfc(Math.abs(realMass-theoreticalMass)/(sd * sqrt2)));
        // prevent infeasible exceptions if the vertex is, for whatever reason, above the allowed
        // ppm
        if (score < -100) {
            LoggerFactory.getLogger(MassDeviationVertexScorer.class).warn("Vertex " + realMass + " has a too large mass deviation of " + Math.abs(realMass-theoreticalMass) + " for molecular formula " + formula + " (" + ion + ").");
            score = -100;
        }
        return score;
    }

    public NormalDistribution getDistribution(double peakMz, double peakIntensity, ProcessedInput input) {
        final double sd = input.getExperimentInformation().getAnnotationOrDefault(MS2MassDeviation.class).standardMassDeviation.absoluteFor(peakMz);
        return new NormalDistribution(0d, sd * sd);
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "useOriginalMz")) this.useOriginalMz = document.getBooleanFromDictionary(dictionary, "useOriginalMz");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "useOriginalMz", useOriginalMz);
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    protected static class Prepared {
        private final SpectralRecalibration recalibration;
        private final Ms1IsotopePattern isotopePattern;
        private final Peak parentPeakFromMs1;

        public Prepared(SpectralRecalibration recalibration, Ms1IsotopePattern isotopePattern, Peak peak) {
            this.recalibration = recalibration;
            this.isotopePattern = isotopePattern;
            this.parentPeakFromMs1 = peak;
        }
    }
}
