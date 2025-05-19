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
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.elementdetection.prediction.DNNRegressionPredictor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

@Requires(Ms1IsotopePattern.class)
@Provides(DetectedFormulaConstraints.class)
public class DeepNeuralNetworkElementDetector implements ElementDetection {

    protected DNNRegressionPredictor dnnRegressionPredictor;

    public DeepNeuralNetworkElementDetector() {
        this.dnnRegressionPredictor = new DNNRegressionPredictor();
    }

    @Override
    @Nullable
    public DetectedFormulaConstraints detect(ProcessedInput processedInput) {
        final FormulaSettings settings = processedInput.getAnnotationOrDefault(FormulaSettings.class);
        checkDetectableElements(settings);
        final PossibleAdducts possibleAdducts = processedInput.getAnnotationOrDefault(PossibleAdducts.class);
        SimpleSpectrum ms1 = processedInput.getAnnotationOrThrow(Ms1IsotopePattern.class).getSpectrum();
        if (ms1.size()<=2) return new DetectedFormulaConstraints(settings.getEnforcedAlphabet().getExtendedConstraints(settings.getFallbackAlphabet()), false);
        final FormulaConstraints constraints = adjustPredictedConstraintsWithAdducts(dnnRegressionPredictor.predictConstraints(ms1), possibleAdducts);
        //limit detection to detectable elements and add enforced alphabet
        return new DetectedFormulaConstraints(constraints.intersection(settings.getAutoDetectionElements().toArray(new Element[0])).getExtendedConstraints(settings.getEnforcedAlphabet()), true);
    }

    /**
     * THIS IS A SAFEGUARD IN CASE ELEMENT PREDICTION ALSO PREDICTS A LOWER BOUND.
     * element prediction can only predict constraints for precursor formula. However, our element constraints are applied to the compound formula.
     * if a possible adduct contains a detectable element, we want to extend the predicted range make sure that predicting an element the comes actually from the adduct and not the compound does not result in an incompatible range for the compound formula
     * //todo element prediction will add unnecessary elements to the compound formula element constraints if the adduct indeed contains this element. we want to differentiate these predicted constraints from input constraints  at some point.
     * @param predictionConstraints
     * @param possibleAdducts
     */
    private FormulaConstraints adjustPredictedConstraintsWithAdducts(FormulaConstraints predictionConstraints, PossibleAdducts possibleAdducts) {
        //if no lower bounds, return original constraints
        if (Arrays.stream(predictionConstraints.getLowerbounds()).max().orElse(0)==0) return predictionConstraints;

        FormulaConstraints adjusted = predictionConstraints.clone();
        predictionConstraints.getChemicalAlphabet().getElements().stream().filter(e-> predictionConstraints.hasElement(e)) //select predicted elements
                .forEach(e -> {
                    int maxCountInAdducts = possibleAdducts.getAdducts().stream().mapToInt(adduct -> adduct.getAdduct().numberOf(e)).max().orElse(0);
                    if (maxCountInAdducts > 0) {
                        adjusted.setLowerbound(e, Math.max(predictionConstraints.getLowerbound(e) - maxCountInAdducts, 0));
                    }
                });

        return adjusted;
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
