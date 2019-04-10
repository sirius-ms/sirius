package de.unijena.bioinf.ms.jobs;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;


public interface AnnotationJJob<A extends Annotated<DataAnnotation>, D extends DataAnnotation> extends JJob<D> {
    A getAnnotatable();

    default D takeAndAnnotateResult() {
        D result = takeResult();
        if (result != null) {
            Class<D> clzz = (Class<D>) result.getClass();
            getAnnotatable().setAnnotation(clzz, result);
        }
        return result;
    }

}
