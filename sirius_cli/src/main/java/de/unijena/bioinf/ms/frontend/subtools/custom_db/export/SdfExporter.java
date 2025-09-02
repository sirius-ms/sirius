package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;

import java.io.BufferedWriter;
import java.io.IOException;

public class SdfExporter extends DbExporter {

    private final SDFWriter sdfWriter;

    public SdfExporter(BufferedWriter writer) {
        sdfWriter = new SDFWriter(writer);
    }

    @Override
    public void write(FingerprintCandidateWrapper candidateWrapper) throws IOException {
        CompoundCandidate c = candidateWrapper.getCandidate(null, null);
        try {
            IAtomContainer molecule = InChISMILESUtils.getAtomContainer(c.getSmiles());
            molecule.setTitle(c.getName());
            molecule.setProperty("NAME", c.getName());
            molecule.setProperty("SMILES", c.getSmiles());
            molecule.setProperty("INCHIKEY2D", c.getInchiKey2D());
            molecule.setProperty("INCHI", c.getInchi().in2D);
            molecule.setProperty("FORMULA", candidateWrapper.getFormula());
            molecule.setProperty("EXACT MASS", candidateWrapper.getMass());
            sdfWriter.write(molecule);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        sdfWriter.close();
    }
}
