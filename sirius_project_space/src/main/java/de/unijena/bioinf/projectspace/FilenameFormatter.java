package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.function.Function;

public interface FilenameFormatter extends Function<Ms2Experiment, String> {
    public String getFormatExpression();
}
