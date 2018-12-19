package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;

import static de.unijena.bioinf.ms.projectspace.SiriusLocations.SIRIUS_SPECTRA;

public class DirectoryWriter implements ProjectWriter {
    protected static final Logger LOG = LoggerFactory.getLogger(DirectoryWriter.class);

    public interface WritingEnvironment {
        void enterDirectory(String name) throws IOException;

        default boolean deleteDirectory(String name) throws IOException {
            throw new UnsupportedOperationException("delete directory");
        }

        OutputStream openFile(String name) throws IOException;

        void closeFile() throws IOException;

        void leaveDirectory() throws IOException;

        void close() throws IOException;

        void updateProgress(String s) throws IOException;

    }

    protected WritingEnvironment env;
    protected MetaDataSerializer[] metaDataWriters;
    //todo do we still need this suppressing stuff -> not adding metainfo writer instead?
    protected final HashSet<String> surpressedOutputs = new HashSet<>();


    public DirectoryWriter(WritingEnvironment w, MetaDataSerializer... metaDataWriters) {
        env = w;
        this.metaDataWriters = metaDataWriters;
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

    @FunctionalInterface
    protected interface Do {
        void run(Writer w) throws IOException;
    }

    protected void write(String name, Do f) throws IOException {
        final OutputStream stream = env.openFile(name);
        try {
            final BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(stream));
            try {
                f.run(new DoNotCloseWriter(outWriter));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                throw e;
            } finally {
                try {
                    outWriter.flush();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } finally {
            env.closeFile();
        }
    }

    @Override
    public boolean deleteExperiment(@NotNull final ExperimentDirectory expDir) throws IOException {
        return env.deleteDirectory(expDir.getDirectoryName());
    }

    @Override
    public void writeExperiment(ExperimentResult result) throws IOException {
        writeInput(result);
        writeMetaData(result);
        env.leaveDirectory();
        env.updateProgress(result.getAnnotation(ExperimentDirectory.class).getDirectoryName() + "\t" + errorCode(result) + "\n");
    }

    protected void writeInput(ExperimentResult result) throws IOException {
        ExperimentDirectory expDir = result.getAnnotation(ExperimentDirectory.class);
        if (expDir == null) throw new IOException("Given experiment result has no ExperimentDirectory Annotation");
        env.enterDirectory(expDir.getDirectoryName());
        // ms file
        if (isAllowed(OutputOptions.INPUT))
            writeMsFile(result);
    }

    protected void writeMetaData(ExperimentResult expResult) {
        for (MetaDataSerializer writer : metaDataWriters) {
            try {
                writer.write(expResult, this);
            } catch (IOException e) {
                LOG.warn("Could not add meta data of " + writer.getClass() + ". Your data might be incomplete.");
            }
        }
    }

    private void writeMsFile(ExperimentResult er) throws IOException {
        // if experiment is stored in results we favour it, as it might be already cleaned and annotated
        final Ms2Experiment experiment = er.getExperiment();
        if (experiment != null) {
            write(SIRIUS_SPECTRA.fileName(), w -> {
                final BufferedWriter bw = new BufferedWriter(w);
                new JenaMsWriter().write(bw, experiment);
                bw.flush();
            });
        }
    }

    private String errorCode(ExperimentResult experiment) {
        if (!experiment.hasError()) return "DONE";
        else return experiment.getErrorString();
    }

    @Override
    public void close() throws IOException {
        env.close();
    }

    public void surpress(String output) {
        surpressedOutputs.add(output);
    }

    public boolean isSurpressed(String name) {
        return surpressedOutputs.contains(name);
    }

    public boolean isAllowed(String name) {
        return !isSurpressed(name);
    }
}
