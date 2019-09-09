package de.unijena.bioinf.ms.annotations;

import de.unijena.bioinf.jjobs.JJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface AnnotationJJob<D extends DataAnnotation, A extends Annotated> extends JJob<D> {
    default D annotate(@Nullable final D result, @NotNull final A annotateable) {
        if (result != null) {
            Class<D> clzz = (Class<D>) result.getClass();
            annotateable.setAnnotation(clzz, result);
        }
        return result;
    }

    default D awaitAndAnnotateResult(@NotNull final A annotateable) throws ExecutionException {
        return annotate(awaitResult(), annotateable);
    }

    default D takeAndAnnotateResult(@NotNull final A annotateable) {
        return annotate(takeResult(), annotateable);
    }
}
