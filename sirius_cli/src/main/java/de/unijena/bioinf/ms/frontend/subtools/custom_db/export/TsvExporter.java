package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.ms.frontend.subtools.summaries.TsvTableWriter;
import de.unijena.bioinf.ms.frontend.utils.DatabaseLinkUtil;
import de.unijena.bioinf.webapi.WebAPI;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TsvExporter extends DbExporter {

    private final TsvTableWriter tsvWriter;
    private final WebAPI<?> api;
    private final boolean withLinks;

    public TsvExporter(BufferedWriter writer, WebAPI<?> api, boolean withLinks) throws IOException {
        this.api = api;
        this.withLinks = withLinks;
        tsvWriter = new TsvTableWriter(writer, false);
        List<String> columns = new ArrayList<>(List.of("name", "SMILES", "InChIkey2D", "InChI", "formula", "mass"));
        if (withLinks) {
            columns.add("links");
        }
        tsvWriter.writeHeader(columns);
    }

    @Override
    public void write(FingerprintCandidateWrapper candidateWrapper) throws IOException {
        tsvWriter.writeRow(extractValues(candidateWrapper));
    }

    private List<Object> extractValues(FingerprintCandidateWrapper cw) throws IOException {
        CompoundCandidate c = cw.getCandidate(null, null);

        List<Object> values = new ArrayList<>(List.of(
                Optional.ofNullable(c.getName()).orElse(""),
                c.getSmiles(),
                c.getInchiKey2D(),
                c.getInchi().in2D,
                cw.getFormula(),
                cw.getMass()));

        if (withLinks) {
            String otherDBLinks = api.applyStructureDB(0, db -> {
                try {
                    List<CompoundCandidate> formulaCandidates = db.lookupStructuresByFormula(InChIs.extractNeutralFormulaByAdjustingHsOrThrow(c.getInchi().in2D));
                    for (CompoundCandidate can : formulaCandidates) {
                        if (can.getInchiKey2D().equals(c.getInchiKey2D())) {
                            Map<String, List<String>> links = can.getLinkedDatabases();
                            return DatabaseLinkUtil.links(links);
                        }
                    }
                    return "";
                } catch (Exception e) {
                    return "";
                }
            });
            values.add(otherDBLinks);
        }

        return values;
    }

    @Override
    public void close() throws IOException {
        tsvWriter.close();
    }
}
