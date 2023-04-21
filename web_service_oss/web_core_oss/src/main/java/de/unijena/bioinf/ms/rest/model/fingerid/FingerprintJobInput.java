package de.unijena.bioinf.ms.rest.model.fingerid;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.fingerid.InputFeatures;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Class containing the input for Fingerprint Jobs
 * Will be (De-)Marshaled to/from json
 * see {@link de.unijena.bioinf.ms.rest.model.JobTable}
 */
public class FingerprintJobInput extends InputFeatures {

    public final EnumSet<PredictorType> predictors;

    private FingerprintJobInput() {
        this(null, null, null, null, null);
    }

    public FingerprintJobInput(InputFeatures source, @NotNull EnumSet<PredictorType> predictors) {
        this(source.tree, source.spectrum, source.neutralizedSpectrum, source.precursorMz, predictors);
    }

    public FingerprintJobInput(@NotNull FTree tree, @NotNull SimpleSpectrum spectrum, @NotNull SimpleSpectrum neutralizedSpectrum, Double precursorMz, @NotNull EnumSet<PredictorType> predictors) {
        super(tree, spectrum, neutralizedSpectrum, precursorMz);
        this.predictors = predictors;
    }
}
