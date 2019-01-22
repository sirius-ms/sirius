package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.projectspace.SiriusLocations.SIRIUS_SPECTRA;


public class DirectoryReader implements ProjectReader {
    protected static final Logger LOG = LoggerFactory.getLogger(DirectoryReader.class);
    private final static Pattern INDEX_PATTERN = Pattern.compile("^(\\d+)_");

    public interface ReadingEnvironment {

        List<String> list();

        void enterDirectory(String name) throws IOException;

        /**
         * test if is this child a directory
         * @param name
         * @return
         */
        boolean isDirectory(String name);

        InputStream openFile(String name) throws IOException;

        URL currentAbsolutePath(String name) throws IOException;

        URL absolutePath(String name) throws IOException;

        void closeFile() throws IOException;

        void leaveDirectory() throws IOException;

        void close() throws IOException;

        default boolean containsFile(@NotNull String dirName, @NotNull String fileName) throws IOException {
            if (!isDirectory(dirName)) return false;
            try {
                enterDirectory(dirName);
                for (String file : list()) {
                    if (file.equals(fileName))
                        return true;
                }
                return false;
            } finally {
                leaveDirectory();
            }
        }

        default <T> T read(@NotNull String name, @NotNull Do<T> f) throws IOException {
            final InputStream stream = openFile(name);
            try {
                final BufferedReader inReader = new BufferedReader(new InputStreamReader(stream));
                try {
                    return f.run(new DirectoryReader.DoNotCloseReader(inReader));
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    throw e;
                }
            } finally {
                closeFile();
            }
        }

        default Map<String, String> readKeyValueFile(@NotNull String name) throws IOException {
            if (!list().contains(name))
                return Collections.emptyMap();

            return read(name, w -> {
                return new BufferedReader(w).lines().filter(l -> l != null && !l.isEmpty()).map(l -> l.split("\t")).collect(Collectors.toMap(k -> k[0], v -> v[1]));
            });

        }

        @FunctionalInterface
        interface Do<T> {
            T run(Reader r) throws IOException;
        }
    }

    protected final ReadingEnvironment env;
    protected final MetaDataSerializer[] metaDataReader;

    public DirectoryReader(ReadingEnvironment env, MetaDataSerializer... metaDataReader) {
        this.env = env;
        this.metaDataReader = metaDataReader;
    }




    public ExperimentResult parseExperiment(final ExperimentDirectory expDir) throws IOException {
        final String directory = expDir.getDirectoryName();
        env.enterDirectory(directory);
        final HashSet<String> names = new HashSet<>(env.list());

        // read spectrum
        final Ms2Experiment input;
        if (names.contains(SIRIUS_SPECTRA.fileName())) {
            input = parseSpectrum();
        } else
            throw new IOException("Invalid Experiment directory. No spectrum.ms found! Your workspace seems to be corrupted.");


        final ExperimentResult expResult;
        if (input.getSource() != null && input.getName() != null) {
            expResult = new ExperimentResult(input, new ArrayList<>());
        } else {
            //fallback for older versions
            String[] nameSplit = directory.split("_");
            String source = nameSplit.length > 1 ? nameSplit[1] : "";
            String name = nameSplit.length > 2 ? nameSplit[2] : "unknown";
            expResult = new ExperimentResult(input, new ArrayList<>(), source, name);
        }
        expResult.setAnnotation(ExperimentDirectory.class, expDir);

        readAndAddMetaData(expResult, names);
        env.leaveDirectory();

        return expResult;
    }


    private Ms2Experiment parseSpectrum() throws IOException {
        return env.read(SIRIUS_SPECTRA.fileName(), r ->
                new JenaMsParser().parse(new BufferedReader(r), env.currentAbsolutePath(SIRIUS_SPECTRA.fileName()))
        );

    }

    @NotNull
    private void parseAndAddIndex(@NotNull final ExperimentDirectory expDir) throws IOException {
        try {
            env.enterDirectory(expDir.getDirectoryName());
            Integer index = null;
            try {
                index = env.read(SiriusLocations.SIRIUS_EXP_INFO_FILE.fileName(), r ->
                        new BufferedReader(r).lines().filter(l -> l.split("\t")[0].equals("index")).findFirst().map(l -> Integer.valueOf(l.split("\t")[1])).orElse(null)
                );
            } catch (IOException e) {
                LOG.warn("Cannot parse index from index file. Cause: " + e.getMessage());
            }

            //parse spectrum ms to find index -> backward compatibility
            if (index == null) {
                expDir.setRewrite(true);

                final Ms2Experiment input = parseSpectrum();
                final String si = input.getAnnotation(AdditionalFields.class, AdditionalFields::new).get("index");
                if (si != null) index = Integer.valueOf(si);

                //parse spectrum ms to find index -> backward compatibility
                if (index == null) {
                    //fallback for older versions
                    Matcher matcher = INDEX_PATTERN.matcher(expDir.getDirectoryName());
                    if (matcher.matches()) {
                        index = Integer.parseInt(matcher.group(1));
                    } else {
                        index = ExperimentDirectory.NO_INDEX;
                        LOG.warn("Cannot parse index for compound in directory " + expDir.getDirectoryName());
                    }
                }
            }
            expDir.setIndex(index);
        } finally {
            env.leaveDirectory();
        }
    }


    private void readAndAddMetaData(ExperimentResult expResult, Set<String> names) {
        for (MetaDataSerializer reader : metaDataReader) {
            try {
                reader.read(expResult, this, names);
            } catch (IOException e) {
                LOG.warn("Could not add meta data of " + reader.getClass() + ". Your data might be incomplete.", e);
            }
        }
    }

    @NotNull
    @Override
    public DirectoryReaderIterator iterator() {
        return new DirectoryReaderIterator();
    }


    @Override
    public void close() throws IOException {
        env.close();
    }

    public class DirectoryReaderIterator implements CloseableIterator<ExperimentDirectory> {
        private final Iterator<String> experiments;
        private final int maxSize;


        private DirectoryReaderIterator() {
            List<String> l = env.list();
            maxSize = l.size();
            this.experiments = l.stream().filter((name) -> {
                try {
                    return env.containsFile(name, SIRIUS_SPECTRA.fileName());
                } catch (IOException e) {
                    throw new RuntimeException("Cannot Enter directory: " + name + System.lineSeparator() + e.getMessage(), e);
                }
            }).iterator();
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public boolean hasNext() {
            return experiments.hasNext();
        }

        @Override
        public ExperimentDirectory next() {
            if (!hasNext()) throw new NoSuchElementException();
            String it = experiments.next();
            try {
                final ExperimentDirectory expDir = new ExperimentDirectory(it);
                parseAndAddIndex(expDir);
                return expDir;
            } catch (IOException e) {
                throw new RuntimeException("Error when parsing index information: " + it + System.lineSeparator() + e.getMessage(), e);
            }
        }

        public int getMaxPossibleSize() {
            return maxSize;
        }
    }


    private static class DoNotCloseReader extends Reader {

        protected final Reader r;

        private DoNotCloseReader(Reader r) {
            this.r = r;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return r.read(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
            // ignore
        }

        @Override
        public int read(CharBuffer target) throws IOException {
            return r.read(target);
        }

        @Override
        public int read() throws IOException {
            return r.read();
        }

        @Override
        public int read(char[] cbuf) throws IOException {
            return r.read(cbuf);
        }

        @Override
        public long skip(long n) throws IOException {
            return r.skip(n);
        }

        @Override
        public boolean ready() throws IOException {
            return r.ready();
        }

        @Override
        public boolean markSupported() {
            return r.markSupported();
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            r.mark(readAheadLimit);
        }

        @Override
        public void reset() throws IOException {
            r.reset();
        }
    }
}
