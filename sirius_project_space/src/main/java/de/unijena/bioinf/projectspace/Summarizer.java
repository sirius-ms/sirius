package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A summarizer does not contain any "new" data, but summarizes or visualize the data of other components.
 * Summarizers can be written automatically with other components.
 */
public interface Summarizer {
    Set<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations();
    void addCompound(@NotNull final Ms2Experiment exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results);
    void writeToProjectSpace(ProjectWriter writer)  throws IOException;

}
