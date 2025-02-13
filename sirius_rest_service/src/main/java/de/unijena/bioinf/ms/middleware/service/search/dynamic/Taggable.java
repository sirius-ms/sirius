package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;

import java.util.Map;

public interface Taggable {
    Map<String, Tag> getTags();
    void setTags(Map<String, Tag> tags);
}
