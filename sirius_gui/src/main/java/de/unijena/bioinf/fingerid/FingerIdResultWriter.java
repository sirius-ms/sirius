package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import gnu.trove.map.hash.TIntFloatHashMap;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class FingerIdResultWriter extends DirectoryWriter {

    protected List<Scored<String>> topHits = new ArrayList<>();

    protected TIntFloatHashMap canopusSummary = new TIntFloatHashMap(2048, 0.75f, -1,-1);
    protected FingerprintVersion canopusVersion = null;

    public FingerIdResultWriter(WritingEnvironment w) {
        super(w, ApplicationCore.VERSION_STRING);
    }

    @Override
    protected void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results) throws IOException {
        super.startWritingIdentificationResults(er, results);
        if (isAllowed(FingerIdResult.CANDIDATE_LISTS) && hasFingerId(results)) {
            // now write CSI:FingerId results
            W.enterDirectory("csi_fingerid");
            final List<FingerIdResult> frs = new ArrayList<>();
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotationOrNull(FingerIdResult.class);
                if (f!=null) {
                    frs.add(f);
                    writeFingerIdResult(result, f);
                }
            }
            W.leaveDirectory();
            if (hasCanopus(results)) {
                W.enterDirectory("canopus");
                for (IdentificationResult result : results) {
                    final CanopusResult r = result.getAnnotationOrNull(CanopusResult.class);
                    if (r != null)
                        writeCanopus(result, r);
                }
                W.leaveDirectory();
            }
            // and CSI:FingerId summary
            writeFingerIdResults(er, results, frs);
        }
    }

    private void writeCanopus(final IdentificationResult result, final CanopusResult r) throws IOException {
        write(makeFileName(result) + ".fpt", new Do() {
            @Override
            public void run(Writer w) throws IOException {
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
            }
        });

    }

    private boolean hasFingerId(List<IdentificationResult> results) {
        for (IdentificationResult r : results) {
            if (r.getAnnotationOrNull(FingerIdResult.class)!=null) return true;
        }
        return false;
    }
    private boolean hasCanopus(List<IdentificationResult> results) {
        for (IdentificationResult r : results) {
            if (r.getAnnotationOrNull(CanopusResult.class)!=null) return true;
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

    private void writeSummary() throws IOException {
        if (isAllowed(FingerIdResult.CANDIDATE_LISTS) && topHits.size()>0) {
            write("summary_csi_fingerid.csv", new Do() {
                @Override
                public void run(Writer w) throws IOException {
                    Collections.sort(topHits, Scored.<String>desc());
                    w.write("source\texperimentName\tconfidence\tinchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
                    for (Scored<String> s : topHits) {
                        w.write(s.getCandidate());
                    }
                }
            });
        }
        /*
        if (canopusSummary.size()>0) {
            writeCanopusSummary();
        }
        */
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
    private void writeFingerIdResults(ExperimentResult er,  List<IdentificationResult> results, final List<FingerIdResult> frs) throws IOException {
        final StringWriter w = new StringWriter(128);
        new CSVExporter().exportFingerIdResults(w, frs);
        final String topHit = w.toString();
        final double confidence = frs.size()>0 ? frs.get(0).getConfidence() : 0;
        final String[] lines = topHit.split("\n",3);
        if (lines.length>=2) {
            topHits.add(new Scored<String>(er.getExperimentSource() + "\t" + er.getExperimentName() + "\t" + confidence + "\t" + lines[1] + "\n", confidence));
        }
        write("summary_csi_fingerid.csv", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                w.write(topHit);
            }
        });
    }

    private void writeFingerIdResult(final IdentificationResult result, final FingerIdResult f) throws IOException {
        write(makeFileName(result) + ".csv", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                new CSVExporter().exportFingerIdResults(w, Arrays.asList(f));
            }
        });
    }
}
