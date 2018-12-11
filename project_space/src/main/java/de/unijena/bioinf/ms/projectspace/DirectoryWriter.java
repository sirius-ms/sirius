package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.Index;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.CSVOutputWriter;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class DirectoryWriter extends AbstractProjectWriter {

    public class Location {
        public final String directory;
        private final String fileName;
        public final String fileExtension;

        public Location(String folder, String fileName, String fileExtension) {
            this.directory = folder;
            this.fileExtension = fileExtension;
            this.fileName = fileName;
        }

        public String fileName() {
            if (fileName == null)
                throw new UnsupportedOperationException("This location name ist IdentificationResult  dependent");

            return fileName + fileExtension;
        }

        public String fileName(IdentificationResult result) {
            if (fileName != null || result == null)
                return fileName();

            return makeFileName(result) + fileExtension;
        }

        public String path(ExperimentResult ex, IdentificationResult result) {
            StringBuilder location = new StringBuilder();
            if (ex != null)
                location.append(makeFileName(ex)).append("/");

            if (directory != null && !directory.isEmpty())
                location.append(directory).append("/");

            location.append(fileName(result));

            return location.toString();
        }
    }

    public class Locations {
        public final Location SIRIUS_TREES_JSON = new Location("trees", null, ".json");
        public final Location SIRIUS_TREES_DOT = new Location("trees", null, ".dot");
        public final Location SIRIUS_ANNOTATED_SPECTRA = new Location("spectra", null, ".ms");
        public final Location SIRIUS_SPECTRA = new Location(null, "spectrum", ".ms");
        public final Location SIRIUS_SUMMARY = new Location(null, "summary_sirius", ".csv");
        public final Location SIRIUS_VERSION_FILE = new Location(null, "version", ".txt");

        public String makeFormulaIdentifier(ExperimentResult ex, IdentificationResult result) {
            return makeFileName(ex) + ":" + result.getMolecularFormula() + ":" + simplify(result.getPrecursorIonType());
        }

        public String makeMassIdentifier(ExperimentResult ex, IdentificationResult result) {
            return makeFileName(ex) + ":" + ex.getExperiment().getIonMass() + ":" + simplify(result.getPrecursorIonType().withoutAdduct());
        }
    }


    private final Locations locations = createLocations();

    protected Locations createLocations() {
        return new Locations();
    }

    protected Locations locations() {
        return locations;
    }

    protected static Logger logger = LoggerFactory.getLogger(DirectoryWriter.class);
    protected int counter = 0;
    protected String currentExperimentName;
    protected WritingEnvironment W;
    private String versionString;
    private FilenameFormatter filenameFormatter;

    protected DirectoryWriter(WritingEnvironment w, String versionString, FilenameFormatter filenameFormatter) {
        W = w;
        this.versionString = versionString;
        this.filenameFormatter = filenameFormatter;
        startWriting();
    }

    @Override
    protected void startWriting() {
    }

    @Override
    public void close() throws IOException {
        endWriting();
        W.close();
    }

    @Override
    protected void endWriting() {
        try {
            writeVersionsInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeVersionsInfo() throws IOException {
        write(locations().SIRIUS_VERSION_FILE.fileName(), new Do() {
            @Override
            public void run(Writer w) throws IOException {
                addVersionStrings(w);
            }
        });
    }

    protected void addVersionStrings(Writer w) {
        try {
            w.write(versionString);
            w.write("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addCitationStrings(Writer w) {
        try {
            w.write(versionString);
            w.write("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class DoNotCloseWriter extends Writer {

        protected final Writer w;

        public DoNotCloseWriter(Writer w) {
            this.w = w;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            w.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            w.flush();
        }

        @Override
        public void write(int c) throws IOException {
            w.write(c);
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            w.write(cbuf);
        }

        @Override
        public void write(String str) throws IOException {
            w.write(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            w.write(str, off, len);
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            return w.append(csq);
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            return w.append(csq, start, end);
        }

        @Override
        public Writer append(char c) throws IOException {
            return w.append(c);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }

    public interface WritingEnvironment {
        void enterDirectory(String name) throws IOException;

        OutputStream openFile(String name) throws IOException;

        void closeFile() throws IOException;

        void leaveDirectory() throws IOException;

        void close() throws IOException;

        void updateProgress(String s) throws IOException;

    }

    protected void write(String name, Do f) throws IOException {
        final OutputStream stream = W.openFile(name);
        try {
            final BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(stream));
            try {
                f.run(new DoNotCloseWriter(outWriter));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw e;
            } finally {
                try {
                    outWriter.flush();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } finally {
            W.closeFile();
        }
    }

    @Override
    protected void writeInput(ExperimentResult result, Ms2Experiment experiment) throws IOException {
        ++counter;
        this.currentExperimentName = makeFileName(result);
        W.enterDirectory(currentExperimentName);
        // ms file
        if (isAllowed(OutputOptions.INPUT))
            writeMsFile(result);
    }

    @Override
    protected void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results) throws IOException {
        // JSON and DOT
        if (isAllowed(OutputOptions.TREES_DOT) || isAllowed(OutputOptions.TREES_JSON)) {
            W.enterDirectory(locations().SIRIUS_TREES_DOT.directory);
            writeTrees(results);
            W.leaveDirectory();
        }
        // CSV
        if (isAllowed(OutputOptions.ANNOTATED_SPECTRA)) {
            W.enterDirectory(locations().SIRIUS_ANNOTATED_SPECTRA.directory);
            writeRecalibratedSpectra(results);
            W.leaveDirectory();
        }
        // formula summary
        writeFormulaSummary(results);
    }

    private void writeMsFile(ExperimentResult er, List<IdentificationResult> results) throws IOException {
        // if experiment is stored in results we favour it, as it might be already cleaned and annotated
        final Ms2Experiment experiment;
        if (results.size() > 0 && results.get(0).getAnnotationOrNull(Ms2Experiment.class) != null) {
            experiment = results.get(0).getAnnotationOrNull(Ms2Experiment.class);
        } else {
            experiment = er.getExperiment();
        }
        if (experiment != null) {
            write(locations().SIRIUS_SPECTRA.fileName(), new Do() {
                @Override
                public void run(Writer w) throws IOException {
                    final BufferedWriter bw = new BufferedWriter(w);
                    new JenaMsWriter().write(bw, experiment);
                    bw.flush();
                }
            });
        }
    }

    private void writeFormulaSummary(final List<IdentificationResult> results) throws IOException {
        write(locations().SIRIUS_SUMMARY.fileName(),
                w -> CSVOutputWriter.writeHits(w, results)
        );
    }

    protected void writeRecalibratedSpectra(List<IdentificationResult> results) throws IOException {
        for (IdentificationResult result : results) {
            writeRecalibratedSpectrum(result);
        }
    }

    protected void writeRecalibratedSpectrum(final IdentificationResult result) throws IOException {
        write(locations().SIRIUS_ANNOTATED_SPECTRA.fileName(result),
                w -> new AnnotatedSpectrumWriter().write(w, result.getRawTree())
        );
    }


    private void writeMsFile(ExperimentResult er) throws IOException {
        // if experiment is stored in results we favour it, as it might be already cleaned and annotated
        final Ms2Experiment experiment = er.getExperiment();
        if (experiment != null) {
            write(locations().SIRIUS_SPECTRA.fileName(), w -> {
                final BufferedWriter bw = new BufferedWriter(w);
                new JenaMsWriter().write(bw, experiment);
                bw.flush();
            });
        }
    }


    protected void writeTrees(List<IdentificationResult> results) throws IOException {
        for (IdentificationResult result : results) {
            writeJSONTree(result);
            writeDOTTree(result);
        }

    }

    private void writeDOTTree(final IdentificationResult result) throws IOException {
        write(locations().SIRIUS_TREES_DOT.fileName(result), w
                -> new FTDotWriter(true, true).writeTree(w, result.getRawTree())
        );
    }

    private void writeJSONTree(final IdentificationResult result) throws IOException {
        write(locations().SIRIUS_TREES_JSON.fileName(result), w ->
                new FTJsonWriter().writeTree(w, result.getResolvedTree())
        );
    }

    @Override
    protected void startWritingIdentificationResult(IdentificationResult result) throws IOException {

    }

    @Override
    protected void writeIdentificationResult(IdentificationResult result) throws IOException {

    }

    @Override
    protected void endWritingIdentificationResult(IdentificationResult result) throws IOException {

    }

    @Override
    protected void endWritingIdentificationResults(List<IdentificationResult> results) throws IOException {

    }

    @Override
    protected void endWritingExperiment(ExperimentResult experiment) throws IOException {
        W.leaveDirectory();
        W.updateProgress(currentExperimentName + "\t" + errorCode(experiment) + "\n");
    }

    private String errorCode(ExperimentResult experiment) {
        if (!experiment.hasError()) return "DONE";
        else return experiment.getErrorString();
    }

    public static String makeFileName(IdentificationResult result) {
        final String filename = result.getRank() + "_" + result.getMolecularFormula() + "_" + simplify(result.getPrecursorIonType());
        return filename;
    }

    private static String simplify(PrecursorIonType precursorIonType) {
        return precursorIonType.toString().replaceAll("[\\[\\] _]", "");
    }

    protected String makeFileName(ExperimentResult exp) {
        final int index = exp.getExperiment().getAnnotation(Index.class, () -> Index.NO_INDEX).index;
        return filenameFormatter.formatName(exp, (index >= 0 ? index : counter));
    }

    protected interface Do {
        void run(Writer w) throws IOException;
    }


}
