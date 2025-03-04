package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;

import java.util.Map;

public interface Taggable {
    String TAG_FIELD_NAME = "tags";

    static String makeTagFieldName(String tagName) {
        return TAG_FIELD_NAME + "." + tagName;
    }

    Map<String, Tag> getTags();
    void setTags(Map<String, Tag> tags);
}
