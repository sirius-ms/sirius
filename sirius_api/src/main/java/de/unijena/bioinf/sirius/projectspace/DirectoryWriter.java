package de.unijena.bioinf.sirius.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
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

    @Override
    public void close() throws IOException {
        W.close();
    }

    public interface WritingEnvironment {
        public abstract void enterDirectory(String name) throws IOException;

        public abstract OutputStream openFile(String name) throws IOException;

        public abstract void closeFile() throws IOException;

        public abstract void leaveDirectory() throws IOException;

        public abstract void close() throws IOException;
    }

    protected void write(String name, Do f)throws IOException  {
        final
        OutputStream stream = W.openFile(name);
        try {
            final BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(stream));
            try {
                f.run(outWriter);
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
    protected void writeInput(Ms2Experiment experiment) throws IOException {
        ++counter;
        this.currentExperimentName = makeFileName(experiment);
        W.enterDirectory(currentExperimentName);
    }

    @Override
    protected void startWritingIdentificationResults(List<IdentificationResult> results)throws IOException  {
        // JSON and DOT
        W.enterDirectory("trees");
        writeTrees(results);
        W.leaveDirectory();
        // CSV
        W.enterDirectory("spectra");
        writeRecalibratedSpectra(results);
        W.leaveDirectory();
        // formula summary
        writeFormulaSummary(results);
    }

    private void writeFormulaSummary(final List<IdentificationResult> results) throws IOException {
        write("formula_summary.csv", new Do() {
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
        write(makeFileName(result), new Do() {
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
    }

    protected String makeFileName(IdentificationResult result) {
        final String filename = result.getRank() + "_" + result.getMolecularFormula();
        return filename;
    }

    protected String makeFileName(Ms2Experiment experiment) {
        final String filename = experiment.getSource().getFile();
        return counter + "_" + filename.substring(0, filename.lastIndexOf('.')) + simplify(experiment.getName());
    }

    private String simplify(String name) {
        return name.replaceAll("[^A-Za-z0-9,\\-]]+", "");
    }

    protected interface Do {
        public void run(Writer w) throws IOException;
    }

}
