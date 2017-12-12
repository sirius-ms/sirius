package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;

public interface AnnotationJJob<R> extends JJob<R> {

    IdentificationResult getIdentificationResult();

    default R takeAndAnnotateResult() {
        R result = takeResult();
        if (result != null) {
            Class<R> clzz = (Class<R>) result.getClass();
            getIdentificationResult().setAnnotation(clzz, result);
        }
        return result;
    }
}
