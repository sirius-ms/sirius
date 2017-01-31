package de.unijena.bioinf.sirius.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.fingerid.CSVExporter;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FingerIdResultWriter extends DirectoryWriter {

    protected List<Scored<String>> topHits = new ArrayList<>();

    public FingerIdResultWriter(WritingEnvironment w) {
        super(w);
    }

    @Override
    protected void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results) throws IOException {
        super.startWritingIdentificationResults(er, results);
        if (isAllowed(FingerIdResult.CANDIDATE_LISTS)) {
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
            // and CSI:FingerId summary
            writeFingerIdResults(er, results, frs);
        }
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
        if (isAllowed(FingerIdResult.CANDIDATE_LISTS)) {
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
    }

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
