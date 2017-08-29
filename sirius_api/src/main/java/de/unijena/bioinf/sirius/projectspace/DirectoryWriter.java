package de.unijena.bioinf.sirius.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.CSVOutputWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class DirectoryWriter extends AbstractProjectWriter {

    protected static Logger logger = LoggerFactory.getLogger(DirectoryWriter.class);
    protected int counter = 0;
    protected String currentExperimentName;
    protected WritingEnvironment W;
    private String versionString;


    public DirectoryWriter(WritingEnvironment w, String versionString) {
        W = w;
        this.versionString = versionString;
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
        write("version.txt", new Do() {
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

    protected void write(String name, Do f)throws IOException  {
        final
        OutputStream stream = W.openFile(name);
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
    }

    @Override
    protected void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results)throws IOException  {
        // ms file
        if (isAllowed(OutputOptions.INPUT))
            writeMsFile(er, results);
        // JSON and DOT
        if (isAllowed(OutputOptions.TREES_DOT) || isAllowed(OutputOptions.TREES_JSON)) {
            W.enterDirectory("trees");
            writeTrees(results);
            W.leaveDirectory();
        }
        // CSV
        if (isAllowed(OutputOptions.ANNOTATED_SPECTRA)) {
            W.enterDirectory("spectra");
            writeRecalibratedSpectra(results);
            W.leaveDirectory();
        }
        // formula summary
        writeFormulaSummary(results);
    }

    private void writeMsFile(ExperimentResult er, List<IdentificationResult> results) throws IOException {
        // if experiment is stored in results we favour it, as it might be already cleaned and annotated
        final Ms2Experiment experiment;
        if (results.size()>0 && results.get(0).getAnnotationOrNull(Ms2Experiment.class)!=null) {
            experiment = results.get(0).getAnnotationOrNull(Ms2Experiment.class);
        } else {
            experiment = er.getExperiment();
        }
        if (experiment!=null) {
            write("spectrum.ms", new Do() {
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
        write("summary_sirius.csv", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                CSVOutputWriter.writeHits(w, results);
            }
        });
    }

    protected void writeRecalibratedSpectra(List<IdentificationResult> results) throws IOException  {
        for (IdentificationResult result : results) {
            writeRecalibratedSpectrum(result);
        }
    }

    protected void writeRecalibratedSpectrum(final IdentificationResult result) throws IOException {
        write(makeFileName(result) + ".ms", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                new AnnotatedSpectrumWriter().write(w, result.getRawTree());
            }
        });
    }

    protected void writeTrees(List<IdentificationResult> results) throws IOException {
        for (IdentificationResult result : results) {
            writeJSONTree(result);
            writeDOTTree(result);
        }

    }

    private void writeDOTTree(final IdentificationResult result)throws IOException  {
        write(makeFileName(result) + ".dot", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                new FTDotWriter(true, true).writeTree(w, result.getRawTree());
            }
        });
    }

    private void writeJSONTree(final IdentificationResult result)throws IOException  {
        write(makeFileName(result) + ".json", new Do() {
            @Override
            public void run(Writer w) throws IOException {
                new FTJsonWriter().writeTree(w, result.getResolvedTree());
            }
        });
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
    protected void endWritingExperiment(Ms2Experiment experiment)throws IOException  {
        W.leaveDirectory();
        W.updateProgress(currentExperimentName + "\tdone.\n");
    }

    public static String makeFileName(IdentificationResult result) {
        final String filename = result.getRank() + "_" + result.getMolecularFormula();
        return filename;
    }

    protected String makeFileName(ExperimentResult exp) {
        return counter + "_" + exp.experimentSource + "_" + exp.experimentName;
    }

    protected interface Do {
        void run(Writer w) throws IOException;
    }

}
