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
package de.unijena.bioinf.sirius.elementpred;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Vote for Br and Cl from the isotope pattern
 */
public class PredictFromMs1 implements Judge {

    private final IsotopePatternAnalysis extractor;

    public PredictFromMs1(IsotopePatternAnalysis extractor) {
        this.extractor = extractor;
    }

    @Override
    public void vote(TObjectIntHashMap<Element> votes, Ms2Experiment experiment, MeasurementProfile profile) {
        final Element Cl =PeriodicTable.getInstance().getByName("Cl");
        final Element Br = PeriodicTable.getInstance().getByName("Br");
        boolean evidenceForClBr = false;
        if (experiment.getMs1Spectra().size() > 0) {
            for (Spectrum<Peak> spec : experiment.getMs1Spectra()) {
                final SimpleSpectrum ms1spec = extractor.extractPattern(spec, profile, experiment.getIonMass());
                final double mono = ms1spec.getMzAt(0);
                final int plus1 = (int)Math.round(mono+1);
                final int plus2 = (int)Math.round(mono+2);
                // find +1 peak
                double int1 = 0, int2 = 0;
                for (int k=0; k < ms1spec.size(); ++k) {
                    if (Math.round(ms1spec.getMzAt(k)) == plus1) {
                        int1 = Math.max(int1, ms1spec.getIntensityAt(k));
                    } else if (Math.round(ms1spec.getMzAt(k))==plus2) {
                        int2 = Math.max(int2, ms1spec.getIntensityAt(k));
                    }
                }
                if (int1==0 && int2 == 0) continue;
                if (int1 > 0 && int2 == 0) {
                    if (!evidenceForClBr) {
                        votes.adjustOrPutValue(Cl, -10, -10);
                        votes.adjustOrPutValue(Br, -10, -10);
                    }
                } else if (int2 > int1) {
                    evidenceForClBr = true;
                    if ((int2 / int1) <= 3 ) {
                        votes.adjustOrPutValue(Cl, 10, 10);
                        votes.adjustOrPutValue(Br, 3, 3);
                    } else if ((int2 / int1) > 3) {
                        votes.adjustOrPutValue(Cl, 5, 5);
                        votes.adjustOrPutValue(Br, 6, 6);
                    }
                } else if (!evidenceForClBr) {
                    votes.adjustOrPutValue(Cl, -10, -10);
                    votes.adjustOrPutValue(Br, -10, -10);
                }

            }
        }
    }
}
