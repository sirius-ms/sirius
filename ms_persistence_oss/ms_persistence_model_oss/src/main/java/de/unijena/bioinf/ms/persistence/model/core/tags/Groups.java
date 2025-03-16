package de.unijena.bioinf.ms.persistence.model.core.tags;

import java.util.Set;

import static de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions.*;

public class Groups {
    private static final String TAG_STRING_VALUE_QUERY = "tags.%s:\"%s\"";

    public static final TagGroup BLANK_RUNS = makeSampleTypeGroup(SAMPLE_TYPE_BLANK);
    public static final TagGroup SAMPLE_RUNS = makeSampleTypeGroup(SAMPLE_TYPE_SAMPLE);
    public static final TagGroup POOLED_QC_RUNS = makeSampleTypeGroup(SAMPLE_TYPE_POOLED_QC);

    public static final Set<TagGroup> DEFAULT_GROUPS = Set.of(BLANK_RUNS, SAMPLE_RUNS, POOLED_QC_RUNS);


    private static TagGroup makeSampleTypeGroup(String sampleType) {
        return TagGroup.builder()
                .groupName(sampleType).editable(false)
                .luceneQuery(String.format(TAG_STRING_VALUE_QUERY, SAMPLE_TYPE.getTagName(), sampleType))
                .build();
    }
}
