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

package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.elementdetection.prediction.DNNRegressionPredictor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Requires(Ms1IsotopePattern.class)
@Provides(FormulaConstraints.class)
public class DeepNeuralNetworkElementDetector implements ElementDetection {

    protected DNNRegressionPredictor dnnRegressionPredictor;

    public DeepNeuralNetworkElementDetector() {
        this.dnnRegressionPredictor = new DNNRegressionPredictor();
    }

    @Override
    @Nullable
    public FormulaConstraints detect(ProcessedInput processedInput) {
        final FormulaSettings settings = processedInput.getAnnotationOrDefault(FormulaSettings.class);
        checkDetectableElements(settings);
        SimpleSpectrum ms1 = processedInput.getAnnotationOrThrow(Ms1IsotopePattern.class).getSpectrum();
        if (ms1.size()<=2) return settings.getEnforcedAlphabet().getExtendedConstraints(settings.getFallbackAlphabet());
        final FormulaConstraints constraints = dnnRegressionPredictor.predictConstraints(ms1);
        //limit detection to detectable elements and add enforced alphabet
        return constraints.intersection(settings.getAutoDetectionElements().toArray(new Element[0])).getExtendedConstraints(settings.getEnforcedAlphabet());
    }

    private void checkDetectableElements(FormulaSettings settings){
        //todo this check is performed for each compound. Rather do it once.
        final ChemicalAlphabet detectable = settings.getAutoDetectionAlphabet();
        for (Element element : detectable) {
            if (!dnnRegressionPredictor.isPredictable(element)) {
                LoggerFactory.getLogger(DeepNeuralNetworkElementDetector.class).warn(element.getSymbol()+" was specified but is not detectable.");
            }
        }
    }

    @Override
    public Set<Element> getPredictableElements() {
        return dnnRegressionPredictor.getChemicalAlphabet().toSet();
    }
}
