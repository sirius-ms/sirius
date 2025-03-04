package de.unijena.bioinf.ms.middleware.service.search.mappers;

import org.apache.lucene.document.Document;

public interface PojoMapper<T> {

    Object getIdValue(T pojo);
    Document toDocument(T pojo);
    T toPojo(Document document);
}
