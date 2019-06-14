package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

public interface FilenameFormatter {

    String formatName(ExperimentResult experimentResult, int index);
    String getFormatExpression();
}
