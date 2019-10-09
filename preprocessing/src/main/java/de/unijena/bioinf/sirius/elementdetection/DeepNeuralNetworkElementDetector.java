package de.unijena.bioinf.sirius.elementdetection;

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
        final FormulaSettings settings = processedInput.getAnnotationOrThrow(FormulaSettings.class);
        SimpleSpectrum ms1 = processedInput.getAnnotationOrThrow(Ms1IsotopePattern.class).getSpectrum();
        if (ms1.size()<=2) return settings.getEnforcedAlphabet().getExtendedConstraints(settings.getFallbackAlphabet());
        final FormulaConstraints constraints = dnnRegressionPredictor.predictConstraints(ms1);
        return settings.getEnforcedAlphabet().getExtendedConstraints(settings.getAutoDetectionElements().toArray(new Element[0])).intersection(constraints);
    }

    @Override
    public Set<Element> getPredictableElements() {
        return dnnRegressionPredictor.getChemicalAlphabet().toSet();
    }
}
