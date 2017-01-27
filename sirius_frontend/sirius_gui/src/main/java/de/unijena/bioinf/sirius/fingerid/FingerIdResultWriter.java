package de.unijena.bioinf.sirius.fingerid;

import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.fingerid.CSVExporter;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FingerIdResultWriter extends DirectoryWriter {


    @Override
    protected void startWritingIdentificationResults(List<IdentificationResult> results) throws IOException {
        super.startWritingIdentificationResults(results);
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
        writeFingerIdResults(results, frs);
    }

    private void writeFingerIdResults(List<IdentificationResult> results, final List<FingerIdResult> frs) throws IOException {
        write("summary_csi_fingerid.csv", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                new CSVExporter().exportFingerIdResults(w, frs);
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
