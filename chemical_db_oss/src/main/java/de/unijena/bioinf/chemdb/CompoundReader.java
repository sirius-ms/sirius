package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;

import java.io.IOException;
import java.io.Reader;

abstract class CompoundReader {

    public abstract CloseableIterator<CompoundCandidate> readCompounds(Reader reader) throws IOException;

    public abstract CloseableIterator<FingerprintCandidate> readFingerprints(FingerprintVersion version, Reader reader) throws IOException;

}
