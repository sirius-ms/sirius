package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;

import java.io.IOException;

public abstract class DbExporter {

    public abstract void write(FingerprintCandidateWrapper candidateWrapper) throws IOException;
    public abstract void close() throws IOException;
}
