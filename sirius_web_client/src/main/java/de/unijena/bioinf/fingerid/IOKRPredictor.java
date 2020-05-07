package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;

import java.io.IOException;

/**
 * This is a dummmy for later IOKR integration
 */
public class IOKRPredictor extends AbstractStructurePredictor {

    protected IOKRPredictor(PredictorType predictorType, WebAPI api) {
        super(predictorType, api);
    }

    @Override
    public void refreshCacheDir() throws IOException {

    }
}
