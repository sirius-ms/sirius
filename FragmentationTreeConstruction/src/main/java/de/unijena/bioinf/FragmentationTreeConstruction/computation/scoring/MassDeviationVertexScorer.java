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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import org.apache.commons.math3.special.Erf;

/**
 * @author Kai Dührkop
 */
@Called("Mass Deviation")
public class MassDeviationVertexScorer implements DecompositionScorer<Object> {
    private final static double sqrt2 = Math.sqrt(2);

    private Deviation standardDeviation = null;
    private boolean useOriginalMz = false;

    public MassDeviationVertexScorer() {
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    public Deviation getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(Deviation standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object _) {
        if (peak.getOriginalPeaks().isEmpty())
            return 0d; // don't score synthetic peaks
        final double theoreticalMass = formula.getMass();
        final double realMass = useOriginalMz ? (peak.getUnmodifiedOriginalMass()) : peak.getUnmodifiedMass();
        final MeasurementProfile profile = input.getExperimentInformation().getMeasurementProfile();
        final Deviation dev = standardDeviation != null ? standardDeviation : profile.getStandardMs2MassDeviation();
        final double sd = dev.absoluteFor(realMass);
        return Math.log(Erf.erfc(Math.abs(realMass-theoreticalMass)/(sd * sqrt2)));
    }

    public NormalDistribution getDistribution(double peakMz, double peakIntensity, ProcessedInput input) {
        final double sd = input.getExperimentInformation().getMeasurementProfile().getStandardMs2MassDeviation().absoluteFor(peakMz);
        return new NormalDistribution(0d, sd*sd);
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "standardDeviation")) {
            this.standardDeviation = Deviation.fromString(document.getStringFromDictionary(dictionary, "standardDeviation"));
        }
        if (document.hasKeyInDictionary(dictionary, "useOriginalMz")) this.useOriginalMz = document.getBooleanFromDictionary(dictionary, "useOriginalMz");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (standardDeviation != null) {
            document.addToDictionary(dictionary, "standardDeviation", standardDeviation.toString());
        }
        document.addToDictionary(dictionary, "useOriginalMz", useOriginalMz);
    }
}
