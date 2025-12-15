package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema
public enum QuantRowType {
    FEATURES(AlignedFeature.class, AlignedFeatures.class, FoldChange.AlignedFeaturesFoldChange.class),
    COMPOUNDS(Compound.class, de.unijena.bioinf.ms.persistence.model.core.Compound.class, FoldChange.CompoundFoldChange.class);

    @JsonIgnore
    private final Class<?> apiTargetClass;
    @JsonIgnore
    private final Class<?> projectTargetClass;
    @JsonIgnore
    private final Class<? extends FoldChange> projectFoldChangeClass;
    @JsonIgnore
    private final String targetIdFieldName;

    QuantRowType(Class<?> apiTargetClass, Class<?> projectTargetClass, Class<? extends FoldChange> projectFoldChangeClass) {
        this.apiTargetClass = apiTargetClass;
        this.projectTargetClass = projectTargetClass;
        this.projectFoldChangeClass = projectFoldChangeClass;
        this.targetIdFieldName = SiriusProjectDatabaseImpl.SIRIUS_PROJECT_METADATA.pkFields.get(projectTargetClass).getName();
    }
}
