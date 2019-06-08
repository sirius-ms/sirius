package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;

public interface Score extends DataAnnotation, Comparable<Score> {
    double score();

    @Override
    default int compareTo(@NotNull Score o) {
        return Double.compare(score(), o.score());
    }
}
