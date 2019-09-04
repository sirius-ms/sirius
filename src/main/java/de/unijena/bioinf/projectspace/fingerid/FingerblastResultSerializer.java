package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FingerblastResultSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FingerblastResult> {

    @Override
    public FingerblastResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final Pattern dblinkPat = Pattern.compile("^.+?: \\(.+\\)$");
        final ArrayList<Scored<FingerprintCandidate>> results = new ArrayList<>();
        reader.table(FingerIdLocations.FingerBlastResults.apply(id),true,(row)->{
            final double score = Double.parseDouble(row[4]);
            final InChI inchi = new InChI(row[0], row[1]);
            final int rank = Integer.parseInt(row[3]);
            final String name = row[5], smiles = row[6];
            final double xlogp = row[7].isBlank() ? Double.NaN : Double.parseDouble(row[7]);
            final PubmedLinks pubmedLinks = PubmedLinks.fromString(row[8]);
            final FingerprintCandidate candidate = new FingerprintCandidate(inchi, null);
            candidate.setName(name);
            candidate.setXlogp(xlogp);
            candidate.setSmiles(smiles);
            final List<DBLink> links = new ArrayList<>();
            long bitset = 0;

            for (String db : row[9].split(";")) {
                Matcher matcher = dblinkPat.matcher(db);
                db = db.trim();
                if (matcher.matches()) {
                    final String dbName = matcher.group(1).trim();
                    for (String dbId : matcher.group(2).split(",")) {
                        links.add(new DBLink(dbName, dbId.trim()));
                    }
                } else {
                    links.add(new DBLink(db,null));
                }
                bitset |= DatasourceService.getDBFlagFromName(db);
            }
            candidate.setLinks(links.toArray(DBLink[]::new));
            candidate.setBitset(bitset);
            results.add(new Scored<FingerprintCandidate>(candidate, score));
        });
        return new FingerblastResult(results);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, FingerblastResult component) throws IOException {
        final String[] header = new String[]{
                "inchikey2D",	"inchi",	"molecularFormula",	"rank",	"score",	"name",	"smiles",	"xlogp",	"PubMedIds",	"links"
        };
        final String[] row = header.clone();
        final int[] ranking = new int[]{0};
        writer.table(FingerIdLocations.FingerBlastResults.apply(id), header, component.getResults().stream().map((hit)->{
            FingerprintCandidate c = hit.getCandidate();
            row[0] = c.getInchiKey2D();
            row[1] = c.getInchi().in2D;
            row[2] = id.getFormula().toString();
            row[3] = String.valueOf(++ranking[0]);
            row[4] = String.valueOf(hit.getScore());
            row[5] = c.getName();
            row[6] = c.getSmiles();
            row[7] = Double.isNaN(c.getXlogp()) ? "" : String.valueOf(c.getXlogp());
            row[8] = c.getPubmedIDs().toString();
            row[9] = c.getLinkedDatabases().asMap().entrySet().stream().map((k)->k.getValue().isEmpty() ? k.getKey() : k.getKey() + ":(" + k.getValue().stream().collect(Collectors.joining(", "))+")").collect(Collectors.joining("; "));
            return row;
        })::iterator);
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(FingerIdLocations.FingerBlastResults.apply(id));
    }
}
