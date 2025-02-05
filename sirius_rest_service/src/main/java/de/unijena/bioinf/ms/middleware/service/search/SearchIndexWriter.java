package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.TaggableLuceneDocumentProvider;
import lombok.SneakyThrows;
import org.apache.lucene.index.IndexableField;

import java.util.Collection;
import java.util.stream.Stream;

public interface SearchIndexWriter {
    @SneakyThrows
    <T extends TaggableLuceneDocumentProvider> void addBean(String projectId, T bean);

    <T extends Iterable<IndexableField>> void addBean(String projectId, T bean);

    @SneakyThrows
    <T extends TaggableLuceneDocumentProvider> void addBeans(String projectId, Collection<T> beans);

    <T extends Iterable<IndexableField>> void addBeans(String projectId, Iterable<T> bean);

}
