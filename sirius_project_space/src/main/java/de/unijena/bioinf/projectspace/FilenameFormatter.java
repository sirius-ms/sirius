package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.function.Function;

public interface FilenameFormatter extends Function<Ms2Experiment, String> {
    String getFormatExpression();

    class ConfigAnnotation implements ProjectSpaceProperty {
        public final String formatExpression;

        public ConfigAnnotation(FilenameFormatter formatter) {
            formatExpression = formatter.getFormatExpression();
        }

        public ConfigAnnotation(String formatExpression) {
            this.formatExpression = formatExpression;
        }
    }
}
