package de.unijena.bioinf.ms.persistence.model.core;

import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersistentSearchIndex {
    @Id
    private String indexKey;

    private byte[] indexData;

    private long storageCommitId;

    public PersistentSearchIndex(String indexKey, byte[] indexData) {
        this.indexKey = indexKey;
        this.indexData = indexData;
        this.storageCommitId = -1;
    }
}
