package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERBLAST;

public class FBCandidatesSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FBCandidates> {

    @Override
    public FBCandidates read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        if (!reader.exists(FINGERBLAST.relFilePath(id)))
            return null;

        final Pattern dblinkPat = Pattern.compile("^.+?:\\(.+\\)$");
        final ArrayList<Scored<CompoundCandidate>> results = new ArrayList<>();
        reader.table(FINGERBLAST.relFilePath(id), true, (row) -> {
            if (row.length == 0) return;
            final double score = Double.parseDouble(row[4]);
            final InChI inchi = InChIs.newInChI(row[0], row[1]);
            final String name = row[5], smiles = row[6];
            final double xlogp = (row[7] != null && !row[7].isBlank() && !row[7].equals("N/A")) ? Double.parseDouble(row[7]) : Double.NaN;

            final CompoundCandidate candidate = new CompoundCandidate(inchi);
            candidate.setName(name);
            candidate.setXlogp(xlogp);
            candidate.setSmiles(smiles);
            candidate.setPubmedIDs(PubmedLinks.fromString(row[8]));

            final List<DBLink> links = new ArrayList<>();
            long linkReconstructuredBitset = 0;

            for (String db : row[9].split(";")) {
                Matcher matcher = dblinkPat.matcher(db);
                db = db.trim();
                if (matcher.matches()) {
                    String[] split = matcher.group().split(":");

                    final String dbName = split[0].trim();
                    final String ids = split[1].trim();
                    for (String dbId : ids.substring(1, ids.length() - 1).split(","))
                        links.add(new DBLink(dbName, dbId.trim()));

                    linkReconstructuredBitset |= DataSources.getDBFlag(dbName);
                } else {
                    LoggerFactory.getLogger(getClass()).warn("Could not match DB link '" + db + "' Skipping this entry!");
                }
            }
            candidate.setLinks(links.toArray(DBLink[]::new));

            if (row.length > 10 && row[10] != null && !row[10].isBlank() && !row[10].equals("N/A")) {
                candidate.setTanimoto(Double.valueOf(row[10]));
            } else
                candidate.setTanimoto(null);

            candidate.setBitset(row.length > 11 && !row[11].isBlank() ? Long.parseLong(row[11]) : linkReconstructuredBitset);

            results.add(new Scored<>(candidate, score));
        });


        return new FBCandidates(results);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FBCandidates> optFingeridResult) throws IOException {
        final FBCandidates fingerblastResult = optFingeridResult.orElseThrow(() -> new IllegalArgumentException("Could not find FingerIdResult to write for ID: " + id));

        final String[] header = new String[]{
                "inchikey2D", "inchi", "molecularFormula", "rank", "score", "name", "smiles", "xlogp", "PubMedIds", "links", "tanimotoSimilarity", "dbflags"
        };
        final String[] row = new String[header.length];
        final AtomicInteger ranking = new AtomicInteger(0);
        writer.table(FINGERBLAST.relFilePath(id), header, fingerblastResult.getResults().stream().map((hit) -> {
            CompoundCandidate c = hit.getCandidate();
            row[0] = c.getInchiKey2D();
            row[1] = c.getInchi().in2D;
            row[2] = id.getMolecularFormula().toString();
            row[3] = String.valueOf(ranking.incrementAndGet());
            row[4] = String.valueOf(hit.getScore());
            row[5] = c.getName();
            row[6] = c.getSmiles();
            row[7] = Double.isNaN(c.getXlogp()) ? "N/A" : String.valueOf(c.getXlogp());
            row[8] = c.getPubmedIDs() != null ? c.getPubmedIDs().toString() : "";
            row[9] = c.getLinkedDatabases().asMap().entrySet().stream().map((k) -> k.getValue().isEmpty() ? k.getKey() : k.getKey() + ":(" + String.join(", ", k.getValue()) + ")").collect(Collectors.joining("; "));
            row[10] = c.getTanimoto() == null ? "N/A" : String.valueOf(c.getTanimoto());
            row[11] = String.valueOf(c.getBitset());
            return row;
        })::iterator);
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(FINGERBLAST.relFilePath(id));
    }
}
