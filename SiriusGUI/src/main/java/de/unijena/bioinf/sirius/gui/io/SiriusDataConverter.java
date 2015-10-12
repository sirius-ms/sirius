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

package de.unijena.bioinf.sirius.gui.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactExperiment;
import de.unijena.bioinf.myxo.structure.DefaultCompactSpectrum;

/**
 * Convert Sirius Data structures to Myxobase data structures
 *
 * @Marvin: Please do not write your own parsers! It is really confusing if for some data types the sirius commandline
 *          tool behaves differently than the sirius gui.
 */
public class SiriusDataConverter {


    public CompactExperiment siriusExperimentToCompactExperiment(Ms2Experiment sirius) {
        final DefaultCompactExperiment exp = new DefaultCompactExperiment();
        exp.setMolecularFormula(sirius.getMolecularFormula());
        exp.setCompoundName(sirius.getName());
        exp.setFocusedMass(sirius.getIonMass());
        exp.setIonization(sirius.getPrecursorIonType().toString());
        exp.setMS1Spectrum(siriusSpectrumToMyxoSpectrum(sirius.getMergedMs1Spectrum()));
        for (Ms2Spectrum<? extends Peak> spec : sirius.getMs2Spectra()) {
            exp.addMS2Spectrum(siriusSpectrumToMyxoSpectrum(spec));
        }
        return exp;
    }

    public CompactSpectrum siriusSpectrumToMyxoSpectrum(Spectrum<? extends Peak> spec) {
        final CompactSpectrum cs = new DefaultCompactSpectrum(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec));
        if (spec instanceof Ms2Spectrum) {
            final Ms2Spectrum<? extends Peak> ms2Spec = (Ms2Spectrum<? extends Peak>)spec;
            cs.setCollisionEnergy(ms2Spec.getCollisionEnergy());
            cs.setMSLevel(ms2Spec.getMsLevel());
        }
        return cs;
    }

}
