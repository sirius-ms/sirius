package de.unijena.bioinf.ms.middleware.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ms.middleware.service.search.LuceneSearchService;
import org.jetbrains.annotations.NotNull;

public interface TaggableLuceneDocumentProvider {

    @JsonIgnore
//    @Schema(hidden = true)
    @NotNull
    LuceneDocument toLuceneDocument(LuceneSearchService.ProjectSearchContext projectSearchContext);
}
