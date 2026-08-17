package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;

import java.util.Map;

public interface Taggable {
    String TAG_FIELD_NAME = "tags";

    static String makeTagFieldName(String tagName) {
        return TAG_FIELD_NAME + "." + tagName;
    }

    /**
     * Inverse of {@link #makeTagFieldName(String)}.
     *
     * @return the tag a search field belongs to, or null if the field is not a tag field. Tag names are free
     * text and may contain dots, so everything behind the prefix is the name.
     */
    static String tagNameOf(String fieldName) {
        String prefix = TAG_FIELD_NAME + ".";
        if (!fieldName.startsWith(prefix) || fieldName.length() == prefix.length())
            return null;
        return fieldName.substring(prefix.length());
    }

    Map<String, Tag> getTags();
    void setTags(Map<String, Tag> tags);
}
