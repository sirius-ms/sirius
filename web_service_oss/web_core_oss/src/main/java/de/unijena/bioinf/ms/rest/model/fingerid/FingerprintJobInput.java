package de.unijena.bioinf.ms.rest.model.fingerid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.fingerid.InputFeatures;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class FingerprintJobInput extends InputFeatures {

    public final EnumSet<PredictorType> predictors;

    public FingerprintJobInput(InputFeatures source, @NotNull EnumSet<PredictorType> predictors) {
        this(source.tree, source.spectrum, source.neutralizedSpectrum, source.precursorMz, predictors);
    }

    public FingerprintJobInput(@NotNull FTree tree, @NotNull SimpleSpectrum spectrum, @NotNull SimpleSpectrum neutralizedSpectrum, double precursorMz, @NotNull EnumSet<PredictorType> predictors) {
        super(tree, spectrum, neutralizedSpectrum, precursorMz);
        this.predictors = predictors;
    }
}
