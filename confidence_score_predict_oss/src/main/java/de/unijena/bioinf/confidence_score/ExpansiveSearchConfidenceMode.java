package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Expansive search parameters.
 * Expansive search will expand the search space to whole PubChem
 * in case no hit with reasonable confidence was found in one of the user specified structure search databases.
 */
public class ExpansiveSearchConfidenceMode implements Ms2ExperimentAnnotation {


    @Schema(enumAsRef = true, name = "ConfidenceMode")
    public enum Mode implements DescriptiveOptions {
        OFF("No expansive search is performed."),
        EXACT("Use confidence score in exact mode: Only molecular structures identical to the true structure should count as correct identification."),//todo NewWorkflow: change description -> should be javadoc instead to allow for automatic usage in config files, CLI and GUI
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

    /**
     * Expansive search mode
     * OFF: No expansive search is performed
     * EXACT: Use confidence score in exact mode: Only molecular structures identical to the true structure should count as correct identification.
     * APPROXIMATE: Use confidence score in approximate mode: Molecular structures hits that are close to the true structure should count as correct identification.
     */
    @DefaultProperty
    public final Mode confidenceScoreSimilarityMode;


    private ExpansiveSearchConfidenceMode() {
        this.confPubChemFactor = -1;
        this.confidenceScoreSimilarityMode=null;
    }
}
