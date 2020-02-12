package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import gnu.trove.list.array.TShortArrayList;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERBLAST;
import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERBLAST_FPs;

public class FingerblastResultSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FingerblastResult> {

    @Override
    public FingerblastResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        if (!reader.exists(FINGERBLAST.relFilePath(id)))
            return null;

        final Pattern dblinkPat = Pattern.compile("^.+?:\\(.+\\)$");
        final ArrayList<Scored<FingerprintCandidate>> results = new ArrayList<>();
        reader.table(FINGERBLAST.relFilePath(id), true, (row) -> {
            if (row.length == 0) return;
            final double score = Double.parseDouble(row[4]);
            final InChI inchi = new InChI(row[0], row[1]);
            final String name = row[5], smiles = row[6];
            final double xlogp = (row[7] != null && !row[7].isBlank() && !row[7].equals("N/A")) ? Double.parseDouble(row[7]) : Double.NaN;

            final FingerprintCandidate candidate = new FingerprintCandidate(inchi, null);
            candidate.setName(name);
            candidate.setXlogp(xlogp);
            candidate.setSmiles(smiles);
            candidate.setPubmedIDs(PubmedLinks.fromString(row[8]));

            final List<DBLink> links = new ArrayList<>();
            long bitset = 0;

            for (String db : row[9].split(";")) {
                Matcher matcher = dblinkPat.matcher(db);
                db = db.trim();
                if (matcher.matches()) {
                    String[] split = matcher.group().split(":");

                    final String dbName = split[0].trim();
                    final String ids = split[1].trim();
                    for (String dbId : ids.substring(1, ids.length() - 1).split(","))
                        links.add(new DBLink(dbName, dbId.trim()));

                    bitset |= DatasourceService.getDBFlagFromName(dbName);
                } else {
                    LoggerFactory.getLogger(getClass()).warn("Could not match DB link '" + db + "' Skipping this entry!");
                }
            }
            candidate.setLinks(links.toArray(DBLink[]::new));
            candidate.setBitset(bitset);
            if (row.length > 10 && row[10] != null && !row[10].isBlank() && !row[10].equals("N/A")) {
                candidate.setTanimoto(Double.valueOf(row[10]));
            } else
                candidate.setTanimoto(null);

            results.add(new Scored<>(candidate, score));
        });

        //read fingerprints from binary
        final FingerIdData fingerIdData = reader.getProjectSpaceProperty(FingerIdData.class).orElseThrow();
        if (reader.exists(FINGERBLAST_FPs.relFilePath(id)) && !results.isEmpty()) {
            reader.binaryFile(FINGERBLAST_FPs.relFilePath(id), br -> {
                try (DataInputStream dis = new DataInputStream(br)) {
                    TShortArrayList shorts = new TShortArrayList(2000); //use it to reconstruct the array
                    int j = 0;
                    while (dis.available() > 0) {
                        short value = dis.readShort();
                        if (value < 0) {
                            results.get(j).getCandidate().setFingerprint(new ArrayFingerprint(fingerIdData.getFingerprintVersion(), shorts.toArray()));
                            shorts.clear();
                            j++;
                        } else {
                            shorts.add(value);
                        }
                    }
                }
                return results;
            });
        }

        return new FingerblastResult(results);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FingerblastResult> optFingeridResult) throws IOException {
        final FingerblastResult fingerblastResult = optFingeridResult.orElseThrow(() -> new IllegalArgumentException("Could not find FingerIdResult to write for ID: " + id));

        final String[] header = new String[]{
                "inchikey2D", "inchi", "molecularFormula", "rank", "score", "name", "smiles", "xlogp", "PubMedIds", "links", "tanimotoSimilarity"
        };
        final String[] row = new String[header.length];
        final AtomicInteger ranking = new AtomicInteger(0);
        writer.table(FINGERBLAST.relFilePath(id), header, fingerblastResult.getResults().stream().map((hit) -> {
            FingerprintCandidate c = hit.getCandidate();
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
            return row;
        })::iterator);

        if (!fingerblastResult.getResults().isEmpty()) {
            writer.binaryFile(FINGERBLAST_FPs.relFilePath(id), (w) -> {
                try (DataOutputStream da = new DataOutputStream(w)) {
                    List<short[]> fpIdxs = fingerblastResult.getResults().stream()
                            .map(SScored::getCandidate).map(FingerprintCandidate::getFingerprint).map(Fingerprint::toIndizesArray)
                            .collect(Collectors.toList());
                    for (short[] fpIdx : fpIdxs) {
                        for (short idx : fpIdx) {
                            da.writeShort(idx);
                        }
                        da.writeShort(-1); //separator
                    }
                }
            });
        }
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(FINGERBLAST.relFilePath(id));
        writer.delete(FINGERBLAST_FPs.relFilePath(id));
    }
}
