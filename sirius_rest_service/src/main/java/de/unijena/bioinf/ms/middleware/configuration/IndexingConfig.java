package de.unijena.bioinf.ms.middleware.configuration;

import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.middleware.service.search.LuceneSearchService;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class IndexingConfig {

    @Bean
    public SearchService searchService(@Value("${de.unijena.bioinf.sirius.indexing.homeDir:#{null}}") Path indexingHome) throws IOException {
        if (indexingHome == null)
            indexingHome = Workspace.WORKSPACE.resolve("search-indexes").resolve("lucene");
        Files.createDirectories(indexingHome);
        return new LuceneSearchService(indexingHome);
    }

}
