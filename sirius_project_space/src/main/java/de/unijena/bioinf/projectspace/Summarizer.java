package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * A summarizer does not contain any "new" data, but summarizes or visualize the data of other components.
 * Summarizers can be written automatically with other components.
 */
public interface Summarizer {
    List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations();

    void addWriteCompoundSummary(ProjectWriter writer, @NotNull final CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results) throws IOException;

    void writeProjectSpaceSummary(ProjectWriter writer) throws IOException;

}
