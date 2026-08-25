package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.service.projects.Project;

public interface SearchContextProvider<P extends Project<?>, SC extends SearchContext> {
    SC create(P project);

    default void destroy(){};
}
