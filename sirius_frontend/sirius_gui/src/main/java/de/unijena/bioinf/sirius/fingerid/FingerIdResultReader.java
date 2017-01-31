package de.unijena.bioinf.sirius.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FingerIdResultReader extends DirectoryReader {

    //TODO: confidence scores are not parsed!

    public FingerIdResultReader(ReadingEnvironment env) {
        super(env);
    }


    private static Pattern DBPAT = Pattern.compile("([^(])+\\(([^)]+)\\)");

    @Override
    protected void addMetaData(Ms2Experiment input, List<IdentificationResult> results) throws IOException {
        super.addMetaData(input, results);
        try {
            env.enterDirectory("csi_fingerid");
            // read compound candidates result list
            final HashSet<String> files = new HashSet<>(env.list());
            for (IdentificationResult r : results) {
                String s = DirectoryWriter.makeFileName(r) +".csv";
                if (files.contains(s)) {
                    r.setAnnotation(FingerIdResult.class, read(s, new Do<FingerIdResult>() {
                        @Override
                        public FingerIdResult run(Reader r) throws IOException {
                            BufferedReader br = new BufferedReader(r);
                            String line = br.readLine();
                            final List<Scored<FingerprintCandidate>> fpcs = new ArrayList<>();
                            while ((line=br.readLine())!=null) {
                                String[] tabs = line.split("\t");
                                final FingerprintCandidate fpc = new FingerprintCandidate(new InChI(tabs[0], tabs[1]), null);
                                fpc.setName(tabs[5]);
                                fpc.setSmiles(tabs[6]);
                                final List<DBLink> links = new ArrayList<>();
                                for (String pubchemId : tabs[8].split(";")) {
                                    links.add(new DBLink(DatasourceService.Sources.PUBCHEM.name, pubchemId));
                                }
                                for (String dbPair : tabs[9].split(";")) {
                                    final Matcher m = DBPAT.matcher(dbPair);
                                    if (m.find()) {
                                        final String dbName = m.group(1);
                                        for (String id : m.group(2).split(" ")) {
                                            links.add(new DBLink(dbName, id));
                                        }
                                    }
                                }
                                fpc.setLinks(links.toArray(new DBLink[links.size()]));
                                fpcs.add(new Scored<FingerprintCandidate>(fpc, Double.parseDouble(tabs[4])));
                            }
                            return new FingerIdResult(fpcs, 0d, null);
                        }
                    }));
                }
            }
        } finally {
            env.leaveDirectory();
        }
    }
}
