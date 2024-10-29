package de.unijena.bioinf.ms.middleware.service.search;

public interface SearchIndexWriter {
    <T> void addBean(String projectId, T bean);
    <T> void addBeans(String projectId, Iterable<T> bean);

}
