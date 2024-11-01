package de.unijena.bioinf.ms.middleware.service.search;

import org.apache.lucene.index.IndexableField;

public interface SearchIndexWriter {
    <T extends Iterable<IndexableField>> void addBean(String projectId, T bean);
    <T extends Iterable<IndexableField>> void addBeans(String projectId, Iterable<T> bean);

}
