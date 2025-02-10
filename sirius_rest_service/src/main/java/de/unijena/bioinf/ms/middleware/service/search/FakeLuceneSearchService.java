package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.lucene.LuceneUtils;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class FakeLuceneSearchService implements SearchService {
    private final Map<String, ProjectSearchContext> projectSearchContexts = new HashMap<>();
    protected final ReadWriteLock projectSpaceLock = new ReentrantReadWriteLock();


    @Override
    public void indexProject(String projectId, SiriusProjectDatabaseImpl<? extends Database<?>> project) {
        projectSpaceLock.writeLock().lock();
        try {
            projectSearchContexts.put(projectId, new ProjectSearchContext(project.findAllTagDefinitionsStr()));
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public void closeProject(String projectId) {
        projectSpaceLock.writeLock().lock();
        try {
            projectSearchContexts.remove(projectId);
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public Stream<TagDefinition> getTagDefinitions(String projectId) {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).tagDefinitions.values().stream();
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public TagDefinition getTagDefinition(String projectId, String tagName) {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).getTagDefinition(tagName);
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }


    @Override
    public void addTagDefinition(String projectId, TagDefinition tagDefinition) {
        projectSpaceLock.readLock().lock();
        try {
            projectSearchContexts.get(projectId).addTagDefinition(tagDefinition);
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public boolean removeTagDefinition(String projectId, String tagName) {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).removeTagDefinition(tagName) != null;
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public Filter parseFindTagsByObjectType(String projectId, Class<?> targeObjectClass, String luceneFilterQuery) throws QueryNodeException, IOException {
        return Filter.and(
                Filter.where("taggedObjectClass").eq(targeObjectClass.getName()),
                parseFindTags(projectId, luceneFilterQuery)
        );

    }

    @Override
    public Filter parseFindTags(String projectId, String luceneFilterQuery) throws QueryNodeException, IOException {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).parseFindTags(luceneFilterQuery);
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public static class ProjectSearchContext {
        private final Map<String, TagDefinition> tagDefinitions;
        @Getter
        private final StandardQueryParser parser;
        protected final ReadWriteLock lock = new ReentrantReadWriteLock();

        public ProjectSearchContext(Stream<TagDefinition> tagDefinitionsStr) {
            this.tagDefinitions = new HashMap<>();
            tagDefinitionsStr = tagDefinitionsStr.peek(tagDef -> tagDefinitions.put(tagDef.getTagName(), tagDef));
            parser = LuceneUtils.makeDefaultQueryParser(tagDefinitionsStr);
        }

        public void addTagDefinition(TagDefinition tagDefinition) {
            lock.writeLock().lock();
            try {
                tagDefinitions.put(tagDefinition.getTagName(), tagDefinition);
                LuceneUtils.updatePointValue(parser.getPointsConfigMap(), tagDefinition);
            } finally {
                lock.writeLock().unlock();
            }

        }

        public TagDefinition removeTagDefinition(String tagName) {
            lock.writeLock().lock();
            try {
                parser.getPointsConfigMap().remove(tagName);
                return tagDefinitions.remove(tagName);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public Filter parseFindTags(String luceneFilterQuery) throws QueryNodeException, IOException {
            lock.readLock().lock();
            try {
                return LuceneUtils.translateTagFilter(luceneFilterQuery, parser, tagDefinitions);
            } finally {
                lock.readLock().unlock();
            }
        }

        public TagDefinition getTagDefinition(String tagName) {
            lock.readLock().lock();
            try {
                return tagDefinitions.get(tagName);
            } finally {
                lock.readLock().unlock();
            }
        }

    }
}
