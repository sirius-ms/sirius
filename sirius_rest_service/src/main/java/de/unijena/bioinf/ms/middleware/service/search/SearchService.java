package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.stream.Stream;

public interface SearchService {

    void indexProject(String projectId, SiriusProjectDatabaseImpl<? extends Database<?>> project);
    void closeProject(String projectId);

    TagDefinition getTagDefinition(String projectId, String tagName);
    Stream<TagDefinition> getTagDefinitions(String projectId);
    void addTagDefinition(String projectId, TagDefinition tagDefinition);
    boolean removeTagDefinition(String projectId, String tagName);

    Filter parseFindTagsByObjectType(String projectId, Class<?> targeObjectClass, String luceneFilterQuery) throws QueryNodeException, IOException;
    Filter parseFindTags(String projectId, String luceneFilterQuery) throws QueryNodeException, IOException;

}
