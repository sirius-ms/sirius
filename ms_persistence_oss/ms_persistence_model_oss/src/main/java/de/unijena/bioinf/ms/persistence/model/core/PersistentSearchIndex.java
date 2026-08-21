package de.unijena.bioinf.ms.persistence.model.core;

import jakarta.persistence.Id;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersistentSearchIndex {
    @Id
    private String indexKey;

    private byte[] indexData;

    /**
     * The version the project's storage was at when this index was written, so a project that moved on without
     * the index moving with it can be told apart from one that did not.
     */
    private long storageCommitId;

    /**
     * How the values in this index were written, and the shape they were written into: what the indexing code
     * looked like, rather than what the data looked like. An index whose either half no longer matches the code
     * reading it is discarded and rebuilt - the shape because a renamed or removed field can no longer answer the
     * queries that name it, the version because the terms under an unchanged field may have been written by rules
     * that have since changed.
     * <p>
     * An index stored before these were recorded reports version 0 and no fingerprint, which is a mismatch with
     * anything and therefore rebuilds once. That is deliberate: it is the only way to be sure about an index
     * whose provenance is unknown.
     */
    private int formatVersion;

    @Nullable
    private String schemaFingerprint;

    public PersistentSearchIndex(String indexKey, byte[] indexData) {
        this.indexKey = indexKey;
        this.indexData = indexData;
        this.storageCommitId = -1;
    }

    /** Whether this index was written by the same index format and schema that are asking for it. */
    public boolean matches(int formatVersion, @NotNull String schemaFingerprint) {
        return this.formatVersion == formatVersion && schemaFingerprint.equals(this.schemaFingerprint);
    }
}
