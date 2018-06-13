package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.babelms.CloseableIterator;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FileCompoundStorage implements SearchStructureByFormula, AnnotateStructures{

    protected File databasePath;
    protected FingerprintVersion version;

    public FileCompoundStorage(File databasePath, FingerprintVersion version) {
        this.databasePath = databasePath;
        this.version = version;
    }

    public FileCompoundStorage(File databasePath) {
        this(databasePath, CdkFingerprintVersion.getComplete());
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws DatabaseException {
        // serialized structures are always annotated
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws DatabaseException {

        final File name = new File(databasePath, formula.toString() + ".json.gz");
        if (name.exists()) {
            try (final GZIPInputStream zin = new GZIPInputStream(new BufferedInputStream(new FileInputStream(name)))) {
                try (final CloseableIterator<FingerprintCandidate> fciter = new JSONReader().readFingerprints(version, new InputStreamReader(zin))) {
                    while (fciter.hasNext()) fingerprintCandidates.add(fciter.next());
                }
            } catch (IOException e) {
                throw new DatabaseException(e);
            }
            return fingerprintCandidates;
        } else {
            return fingerprintCandidates;
        }

    }
}
