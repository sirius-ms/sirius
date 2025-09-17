package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.ms.frontend.subtools.summaries.TsvTableWriter;
import de.unijena.bioinf.ms.frontend.utils.DatabaseLinkUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class TsvExporter extends DbExporter {

    private final TsvTableWriter tsvWriter;

    public TsvExporter(BufferedWriter writer) throws IOException {
        tsvWriter = new TsvTableWriter(writer, false);
        tsvWriter.writeHeader(List.of("name", "SMILES", "InChIkey2D", "InChI", "formula", "mass", "links"));
    }

    @Override
    public void write(FingerprintCandidateWrapper candidateWrapper) throws IOException {
        tsvWriter.writeRow(extractValues(candidateWrapper));
    }

    private List<Object> extractValues(FingerprintCandidateWrapper cw) throws IOException {
        CompoundCandidate c = cw.getCandidate(null, null);
        return List.of(
                c.getName(),
                c.getSmiles(),
                c.getInchiKey2D(),
                c.getInchi().in2D,
                cw.getFormula(),
                cw.getMass(),
                DatabaseLinkUtil.links(c.getLinkedDatabases())
        );
    }

    @Override
    public void close() throws IOException {
        tsvWriter.close();
    }
}
