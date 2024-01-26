package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;

public class ExpansiveSearchConfidenceMode {


    public enum Mode implements DescriptiveOptions {
        OFF("No expansive search is performed."),
        EXACT("Use confidence score in exact mode: Only molecular structures identical to the true structure should count as correct identification."),//todo NewWorkflow: change description
        APPROXIMATE("Use confidence score in approximate mode: Molecular structures hits that are close to the true structure should count as correct identification.");

        private final String description;

        Mode(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public static Mode[] getActiveModes() {
            return new Mode[]{EXACT, APPROXIMATE};
        }
    }


    public final Mode confidenceScoreSimilarityMode;


    //todo NewWorkflow: describe
    public ExpansiveSearchConfidenceMode(Mode confidenceScoreSimilarityMode) {
        this.confidenceScoreSimilarityMode = confidenceScoreSimilarityMode;
    }

    @DefaultInstanceProvider
    public static ExpansiveSearchConfidenceMode newInstance(Mode mode) {
        return new ExpansiveSearchConfidenceMode(mode);
    }
}
