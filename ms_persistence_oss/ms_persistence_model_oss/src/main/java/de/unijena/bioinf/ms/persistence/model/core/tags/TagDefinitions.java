package de.unijena.bioinf.ms.persistence.model.core.tags;

import java.util.List;
import java.util.Set;

public class TagDefinitions {
    public static final String SAMPLE_TYPE_BLANK = "Blank";
    public static final String SAMPLE_TYPE_SAMPLE = "Sample";
    public static final String SAMPLE_TYPE_POOLED_QC = "Pooled QC";

    public static final TagDefinition SAMPLE_TYPE = TagDefinition.builder()
            .tagName("runType").tagType("IMPORT").editable(false)
            .valueDefinition(new ValueDefinition<>(ValueType.TEXT, List.of(SAMPLE_TYPE_SAMPLE, SAMPLE_TYPE_BLANK, SAMPLE_TYPE_POOLED_QC), null, null))
            .displayName("MS Run Type")
            .description("Defines type of a MS run (e.g. blank, sample, qc) to specify handling during import an preprocessing.")
            .build();

    public static final Set<TagDefinition> DEFAULT_TAG_DEFINITIONS = Set.of(SAMPLE_TYPE);
}
