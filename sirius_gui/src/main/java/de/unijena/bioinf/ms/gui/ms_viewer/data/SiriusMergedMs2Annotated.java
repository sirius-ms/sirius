/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.ms_viewer.data;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusMergedMs2Annotated extends SiriusSingleSpectrumAnnotated {

    public SiriusMergedMs2Annotated(FTree tree, Ms2Experiment experiment, double minMz, double maxMz) {
        super(tree, merge(experiment, tree), minMz, maxMz);
    }

    public SiriusMergedMs2Annotated(FTree tree, Ms2Experiment experiment) {
        super(tree, merge(experiment, tree));
    }

    private static Spectrum<? extends Peak> merge(Ms2Experiment experiment, FTree tree) {
//        ProcessedInput processedInput = tree.getAnnotationOrNull(ProcessedInput.class);
        System.out.println("Implement merged peaks stuff"); //todo reimplement
       /* if (false){
            List<ProcessedPeak> peakList = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class).;
            SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
            for (ProcessedPeak processedPeak : peakList) {
                spectrum.addPeak(processedPeak);
            }
            return spectrum;
        }*/
        return Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, experiment.getMs2Spectra());
    }
}
