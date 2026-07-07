package de.unijena.bioinf.ms.persistence.model.core.tags;

import java.util.List;
import java.util.Set;

public class TagDefinitions {
    /*
    SAMPLE TYPE
     */
    public static final String SAMPLE_TYPE_BLANK = "Blank";
    public static final String SAMPLE_TYPE_SAMPLE = "Sample";
    public static final String SAMPLE_TYPE_POOLED_QC = "Pooled QC";

    public static final TagDefinition SAMPLE_TYPE = TagDefinition.builder()
            .tagName("runType").tagType("IMPORT").editable(false)
            .valueDefinition(new ValueDefinition<>(ValueType.TEXT, List.of(SAMPLE_TYPE_SAMPLE, SAMPLE_TYPE_BLANK, SAMPLE_TYPE_POOLED_QC), null, null))
            .displayName("MS Run Type")
            .description("Defines type of a MS run (e.g. blank, sample, qc) to specify handling during import an preprocessing.")
            .build();

    /*
    HOMOLOGUE SERIES AND PFAS
     */
    public static final String PFAS_TYPE_0 = "Potential PFAS";
    public static final String PFAS_TYPE_1 = "PFAS Molecular Formula";
    public static final String PFAS_TYPE_2 = "PFAS Molecular Structure";
    public static final TagDefinition PFAS_TYPE = TagDefinition.builder()
            .tagName("pfas").tagType("IMPORT").editable(false)
            .valueDefinition(new ValueDefinition<>(ValueType.TEXT, List.of(PFAS_TYPE_0, PFAS_TYPE_1, PFAS_TYPE_2), null, null))
            .displayName("Potential PFAS detected")
            .description("For features which are part of a PFAS homologue series, PFAS isotope pattern or for which SIRIUS or CSI found a PFAS molecular formula/structure.")
            .build();

    public static final Set<TagDefinition> DEFAULT_TAG_DEFINITIONS = Set.of(SAMPLE_TYPE, PFAS_TYPE);
}
