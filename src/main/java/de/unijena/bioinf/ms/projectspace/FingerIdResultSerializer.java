package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.CSVExporter;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FingerIdResultSerializer implements MetaDataSerializer, SummaryWriter {
    private static Pattern DBPAT = Pattern.compile("([^(])+\\(([^)]+)\\)");

    @Override //todo read fingerid results
    public void read(@NotNull final ExperimentResult result, @NotNull final DirectoryReader reader, @NotNull Set<String> names) throws IOException {
        final DirectoryReader.ReadingEnvironment env = reader.env;
        final List<IdentificationResult> results = result.getResults();

        if (!new HashSet<>(env.list()).contains("csi_fingerid")) return;

        try {
            env.enterDirectory("csi_fingerid");
            // read compound candidates identificationResult list
            final HashSet<String> files = new HashSet<>(env.list());
            for (IdentificationResult r : results) {
                String s = SiriusLocations.makeFileName(r) + ".csv";
                if (files.contains(s)) {
                    r.setAnnotation(FingerIdResult.class, env.read(s, r1 -> {
                        BufferedReader br = new BufferedReader(r1);
                        String line = br.readLine();
                        final List<Scored<FingerprintCandidate>> fpcs = new ArrayList<>();
                        while ((line = br.readLine()) != null) {
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
                        // TODO: implement
                        return new FingerIdResult(fpcs, 0d, null, null);
                    }));
                }
            }
        } finally {
            env.leaveDirectory();
        }
    }


    @Override
    public void write(@NotNull final ExperimentResult input, @NotNull final DirectoryWriter writer) throws IOException {
        final DirectoryWriter.WritingEnvironment W = writer.env;
        final List<IdentificationResult> results = input.getResults();

        if (writer.isAllowed(FingerIdResult.CANDIDATE_LISTS) && hasFingerId(results)) {
            // now write CSI:FingerID results

            W.enterDirectory(FingerIdLocations.FINGERID_FINGERPRINT.directory);
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotationOrNull(FingerIdResult.class);
                if (f != null) {
                    writeFingerprint(result, f, writer);
                }
            }
            W.leaveDirectory();


            W.enterDirectory(FingerIdLocations.FINGERID_CANDIDATES.directory);
            final List<FingerIdResult> frs = new ArrayList<>();
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotationOrNull(FingerIdResult.class);
                if (f != null) {
                    frs.add(f);
                    writeFingerIdResult(result, f, writer);
                }
            }
            W.leaveDirectory();

            // and CSI:FingerID summary
            writeFingerIdResultsSummaryCSV(frs, writer);
        }
    }

    private void writeFingerprint(IdentificationResult result, FingerIdResult f, DirectoryWriter writer) throws IOException {
        writer.write(FingerIdLocations.FINGERID_FINGERPRINT.fileName(result), w -> {
            for (FPIter fp : f.getPredictedFingerprint()) {
                w.write(String.format(Locale.US, "%.3f\n", fp.getProbability()));
            }
        });
    }

    private boolean hasFingerId(List<IdentificationResult> results) {
        for (IdentificationResult r : results) {
            if (r.getAnnotationOrNull(FingerIdResult.class) != null) return true;
        }
        return false;
    }

    private void writeFingerIdResultsSummaryCSV(final List<FingerIdResult> frs, DirectoryWriter writer) throws IOException {
        final StringWriter w = new StringWriter(128);
        new CSVExporter().exportFingerIdResults(w, frs);
        writer.write(FingerIdLocations.FINGERID_SUMMARY.fileName(), w1 -> w1.write(w.toString()));
    }

    private void writeFingerIdResult(final IdentificationResult result, final FingerIdResult f, DirectoryWriter writer) throws IOException {
        writer.write(FingerIdLocations.FINGERID_CANDIDATES.fileName(result), w ->
                new CSVExporter().exportFingerIdResults(w, Arrays.asList(f))
        );
    }

    @Override
    public void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer) {
        try {
            FingerprintVersion csiVersion = null; //todo this is ugly and error prone
            final List<Scored<String>> topHits = new ArrayList<>();
            if (writer.isAllowed(FingerIdResult.CANDIDATE_LISTS)) {
                for (ExperimentResult experimentResult : experiments) {
                    final List<IdentificationResult> results = experimentResult.getResults();
                    if (hasFingerId(results)) {
                        final List<FingerIdResult> frs = results.stream().map((r) -> r.getAnnotationOrNull(FingerIdResult.class))
                                .filter(Objects::nonNull).collect(Collectors.toList());
                        final StringWriter w = new StringWriter(128);
                        new CSVExporter().exportFingerIdResults(w, frs);
                        final String topHit = w.toString();
                        final double confidence = frs.size() > 0 ? frs.get(0).getConfidence() : 0;
                        final String[] lines = topHit.split("\n", 3);
                        if (lines.length >= 2) {
                            topHits.add(new Scored<>(experimentResult.getExperimentSource() + "\t" + experimentResult.getExperimentName() + "\t" + confidence + "\t" + lines[1] + "\n", confidence));
                        }

                        if (csiVersion == null && !frs.isEmpty())
                            csiVersion = frs.get(0).getPredictedFingerprint().getFingerprintVersion();
                    }
                }

                if (topHits.size() > 0) {
                    writeSummaryCSV(topHits, writer);
                }
                writeFingerprintIndex(FingerIdLocations.FINGERID_FINGERPRINT_INDEX.fileName(), csiVersion, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void writeSummaryCSV(List<Scored<String>> topHits, DirectoryWriter writer) throws IOException {
        writer.write(FingerIdLocations.FINGERID_SUMMARY.fileName(), w -> {
            Collections.sort(topHits, Scored.<String>desc());
            w.write("source\texperimentName\tconfidence\tinchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
            for (Scored<String> s : topHits) {
                w.write(s.getCandidate());
            }
        });
    }

    private void writeFingerprintIndex(String filename, FingerprintVersion version, DirectoryWriter writer) throws IOException {
        if (version == null) return;
        writer.write(filename, (w) -> {
            final int[] indizes;
            if (version instanceof MaskedFingerprintVersion) {
                indizes = ((MaskedFingerprintVersion) version).allowedIndizes();
            } else {
                indizes = new int[version.size()];
                for (int i = 0; i < indizes.length; ++i) indizes[i] = i;
            }
            w.write("relativeIndex\tabsoluteIndex\tdescription\n");
            int k = 0;
            for (int index : indizes) {
                final MolecularProperty prop = (MolecularProperty) version.getMolecularProperty(index);
                w.write(String.valueOf(k++));
                w.write('\t');
                w.write(String.valueOf(index));
                w.write('\t');
                w.write(prop.getDescription());
                w.write('\n');
            }
        });
    }
    //protected MztabMExporter mztabMExporter = new MztabMExporter(locations());
    //System.out.println("Adding result to report mztab");
    //mztabMExporter.addExperiment(er, results);
    //System.out.println("Writing Summary mztab");
    //write(locations().WORKSPACE_SUMMARY.fileName(), w -> mztabMExporter.write(w));
}
