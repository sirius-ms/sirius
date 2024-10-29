package de.unijena.bioinf.ms.middleware.service.search;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchIndexReader {
   <T> Page<String> search(String projectId, String query, Pageable paging, Class<T> beanClass, @NotNull String idField, @NotNull String defaultField);
}
