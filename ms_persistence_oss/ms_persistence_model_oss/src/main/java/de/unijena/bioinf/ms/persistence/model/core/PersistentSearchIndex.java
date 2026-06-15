package de.unijena.bioinf.ms.persistence.model.core;

import jakarta.persistence.Id;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@AllArgsConstructor
public class PersistentSearchIndex {
    @Id
    private String indexKey;

    private byte[] indexData;
}
