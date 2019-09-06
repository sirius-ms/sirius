package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;


public interface AnnotationJJob<A extends Annotated<D>, D extends DataAnnotation> extends JJob<D> {
    default D takeAndAnnotateResult(@NotNull final A annotateable) {
        D result = takeResult();
        if (result != null) {
            Class<D> clzz = (Class<D>) result.getClass();
            annotateable.setAnnotation(clzz, result);
        }
        return result;
    }

}
