package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;


public class ExpansiveSearchConfidenceMode implements Ms2ExperimentAnnotation {



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
    /**
     * Factor that PubChem confidence scores gets multiplied with as bias against it.
     */
     @DefaultProperty
    public final double confPubChemFactor;


    @DefaultProperty
    public final Mode confidenceScoreSimilarityMode;


    private ExpansiveSearchConfidenceMode() {
        this.confPubChemFactor = -1;
        this.confidenceScoreSimilarityMode=null;
    }
}
