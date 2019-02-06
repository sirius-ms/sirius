package de.unijena.bioinf.sirius;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

public interface IdentificationResultAnnotationJJob<R extends DataAnnotation> extends JJob<R> {

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