package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.io.MztabMExporter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.FilenameFormatter;
import gnu.trove.map.hash.TIntFloatHashMap;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class FingerIdResultWriter extends DirectoryWriter {


    public class Locations extends DirectoryWriter.Locations {
        public final Location FINGERID_FINGERPRINT = new Location("fingerprints", null, ".fpt");
        public final Location FINGERID_FINGERPRINT_INDEX = new Location(null, "fingerprints", ".csv");
        public final Location FINGERID_CANDIDATES = new Location("csi_fingerid", null, ".csv");
        public final Location FINGERID_SUMMARY = new Location(null, "summary_csi_fingerid", ".csv");

        public final Location CANOPUS_FINGERPRINT = new Location("canopus", null, ".fpt");
        public final Location CANOPUS_FINGERPRINT_INDEX = new Location(null, "canopus", ".csv");

        public final Location WORKSPACE_SUMMARY = new Location(null, "analysis_report", ".mztab");
    }

    @Override
    protected DirectoryWriter.Locations createLocations() {
        return new Locations();
    }

    @Override
    protected Locations locations() {
        return (Locations) super.locations();
    }

    protected List<Scored<String>> topHits = new ArrayList<>();

    protected TIntFloatHashMap canopusSummary = new TIntFloatHashMap(2048, 0.75f, -1, -1);
    protected FingerprintVersion canopusVersion = null;
    protected FingerprintVersion csiVersion = null;
    protected MztabMExporter mztabMExporter = new MztabMExporter(locations());

    public FingerIdResultWriter(WritingEnvironment w, FilenameFormatter filenameFormatter) {
        super(w, ApplicationCore.VERSION_STRING, filenameFormatter);
    }

    @Override
    protected void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results) throws IOException {
        super.startWritingIdentificationResults(er, results);

        System.out.println("Adding result to report mztab");
        mztabMExporter.addExperiment(er, results);

        if (isAllowed(FingerIdResult.CANDIDATE_LISTS) && hasFingerId(results)) {
            // now write CSI:FingerID results
            W.enterDirectory(locations().FINGERID_FINGERPRINT.directory);
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotationOrNull(FingerIdResult.class);
                if (f != null) {
                    writeFingerprint(result, f);
                }
            }
            W.leaveDirectory();
            W.enterDirectory(locations().FINGERID_CANDIDATES.directory);
            final List<FingerIdResult> frs = new ArrayList<>();
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotationOrNull(FingerIdResult.class);
                if (f != null) {
                    frs.add(f);
                    writeFingerIdResult(result, f);
                }
            }
            W.leaveDirectory();
            if (hasCanopus(results)) {
                W.enterDirectory(locations().CANOPUS_FINGERPRINT.directory);
                for (IdentificationResult result : results) {
                    final CanopusResult r = result.getAnnotationOrNull(CanopusResult.class);
                    if (r != null)
                        writeCanopus(result, r);
                }
                W.leaveDirectory();
            }
            // and CSI:FingerID summary
            writeFingerIdResults(er, results, frs);
        }
    }

    private void writeFingerprint(IdentificationResult result, FingerIdResult f) throws IOException {
        if (csiVersion == null) csiVersion = f.predictedFingerprint.getFingerprintVersion();
        write(locations().FINGERID_FINGERPRINT.fileName(result), w -> {
            for (FPIter fp : f.predictedFingerprint) {
                w.write(String.format(Locale.US, "%.3f\n", fp.getProbability()));
            }
        });
    }

    private void writeCanopus(final IdentificationResult result, final CanopusResult r) throws IOException {
        if (canopusVersion == null) canopusVersion = r.canopusFingerprint.getFingerprintVersion();
        write(locations().CANOPUS_FINGERPRINT.fileName(result), w -> {
            /*
            if (canopusSummary.isEmpty()) {
                for (FPIter iter : r.getCanopusFingerprint()) {
                    canopusSummary.adjustValue(iter.getIndex(), 0);
                }
                canopusVersion = r.getCanopusFingerprint().getFingerprintVersion();
            }
            */
            for (FPIter iter : r.getCanopusFingerprint()) {
                w.write(String.format(Locale.US, "%.2f\n", iter.getProbability()));
                /*
                float prob = (float)iter.getProbability();
                if (prob < 0.1f) prob = 0f;
                if (prob > 0.9f) prob = 1f;
                if (prob > 0f)
                    canopusSummary.adjustOrPutValue(iter.getIndex(), prob, prob);
                    */
            }
        });

    }

    private boolean hasFingerId(List<IdentificationResult> results) {
        for (IdentificationResult r : results) {
            if (r.getAnnotationOrNull(FingerIdResult.class) != null) return true;
        }
        return false;
    }

    private boolean hasCanopus(List<IdentificationResult> results) {
        for (IdentificationResult r : results) {
            if (r.getAnnotationOrNull(CanopusResult.class) != null) return true;
        }
        return false;
    }


    @Override
    protected void endWriting() {
        super.endWriting();
        try {
            writeSummary();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated //this is replaced byt the mztab summary
    private void writeSummaryCSV() throws IOException {
        write(locations().FINGERID_SUMMARY.fileName(), w -> {
            Collections.sort(topHits, Scored.<String>desc());
            w.write("source\texperimentName\tconfidence\tinchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
            for (Scored<String> s : topHits) {
                w.write(s.getCandidate());
            }
        });
    }

    private void writeSummary() throws IOException {
        if (isAllowed(FingerIdResult.CANDIDATE_LISTS) && topHits.size() > 0) {
            writeSummaryCSV();
            System.out.println("Writing Summary mztab");
        }
        write(locations().WORKSPACE_SUMMARY.fileName(), w -> mztabMExporter.write(w));

        /*
        if (canopusSummary.size()>0) {
            writeCanopusSummary();
        }
        */

        // write fingerprint version
        writeFingerprintIndex(locations().FINGERID_FINGERPRINT_INDEX.fileName(), csiVersion);
        writeCanopusIndex(locations().CANOPUS_FINGERPRINT_INDEX.fileName(), canopusVersion);


    }

    private void writeFingerprintIndex(String filename, FingerprintVersion version) throws IOException {
        if (version == null) return;
        write(filename, (w) -> {
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

    private void writeCanopusIndex(String filename, FingerprintVersion version) throws IOException {
        if (version == null) return;
        write(filename, (w) -> {
            final int[] indizes;
            if (version instanceof MaskedFingerprintVersion) {
                indizes = ((MaskedFingerprintVersion) version).allowedIndizes();
            } else {
                indizes = new int[version.size()];
                for (int i = 0; i < indizes.length; ++i) indizes[i] = i;
            }
            w.write("relativeIndex\tabsoluteIndex\tname\tchemontId\tdescription\n");
            int k = 0;
            for (int index : indizes) {
                final ClassyfireProperty prop = (ClassyfireProperty) version.getMolecularProperty(index);
                w.write(String.valueOf(k++));
                w.write('\t');
                w.write(String.valueOf(index));
                w.write('\t');
                w.write(prop.getName());
                w.write('\t');
                w.write(prop.getChemontIdentifier());
                w.write('\t');
                w.write(prop.getDescription().replaceAll("[\t\n]", " "));
                w.write('\n');
            }
        });
    }

    /*
        private void writeCanopusSummary() throws IOException {
            final StringWriter string = new StringWriter(2048);
            JsonWriter writer = new JsonWriter(string);
            writer.beginObject();
            write("summary_canopus.csv", new Do() {
                @Override
                public void run(Writer w) throws IOException {
                    w.write("index\tid\tname\texpectedFrequency\tdescription\n");
                    final int[] indizes = canopusSummary.keys();
                    Arrays.sort(indizes);
                    int k=0;
                    for (int index : indizes) {
                        final ClassyfireProperty prop = (ClassyfireProperty) canopusVersion.getMolecularProperty(index);
                        w.write(String.format(Locale.US, "%d\tCHEMONT:%07d\t%s\t%.1f\t%s\n", k, prop.getChemOntId(), prop.getName(), canopusSummary.get(index), prop.getDescription() ));
                    }
                }
            });
            write("summary_canopus.html", new Do() {
                @Override
                public void run(Writer w) throws IOException {
                    final String html = FileUtils.read(FileUtils.resource(FingerIdResultWriter.class, "/sirius/canopus.html.gz"));
                    html.replace("null;//{{INSERT JSON DATA HERE}}//", json));
                }
            });
        }
    */

    private void writeFingerIdResults(ExperimentResult er, final List<IdentificationResult> results, final List<FingerIdResult> frs) throws IOException {
        final StringWriter w = new StringWriter(128);
        new CSVExporter().exportFingerIdResults(w, frs);
        final String topHit = w.toString();
        final double confidence = frs.size() > 0 ? frs.get(0).getConfidence() : 0;
        final String[] lines = topHit.split("\n", 3);
        if (lines.length >= 2) {
            topHits.add(new Scored<String>(er.getExperimentSource() + "\t" + er.getExperimentName() + "\t" + confidence + "\t" + lines[1] + "\n", confidence));
        }

        write(locations().FINGERID_SUMMARY.fileName(), w1 -> w1.write(topHit));

    }

    private void writeFingerIdResult(final IdentificationResult result, final FingerIdResult f) throws IOException {
        write(locations().FINGERID_CANDIDATES.fileName(result), w ->
                new CSVExporter().exportFingerIdResults(w, Arrays.asList(f))
        );
    }
}
