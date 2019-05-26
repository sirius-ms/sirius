package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class ZippedSpectraParser extends GenericParser<Ms2Experiment> {
    private final MsExperimentParser msExperimentParser;

    public ZippedSpectraParser() {
        super(null);
        msExperimentParser = new MsExperimentParser();
    }

    @Override
    public <S extends Ms2Experiment> List<S> parseFromFile(File file) throws IOException {
        BufferedReader reader = null;
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        final ArrayList<S> list = new ArrayList<S>();
        ZipEntry entry = null;

        try {
            while(entries.hasMoreElements()){
                entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                File asFile = new File(entry.getName());
                final GenericParser<Ms2Experiment> genericParser = msExperimentParser.getParser(asFile);
                InputStream stream = zipFile.getInputStream(entry);
                reader = FileUtils.ensureBuffering(new InputStreamReader(stream));
                final URL source = file.toPath().resolve(entry.getName()).toUri().toURL();

                S elem = genericParser.parse(reader,source);
                while (elem!=null) {
                    list.add(elem);
                    elem = genericParser.parse(reader,source);
                }
                reader.close();
            }
            return list;
        } catch (IOException e) {
            final IOException newOne = new IOException("Error while parsing " + entry.getName() + " in zip archive " + file.getName(), e);
            throw newOne;
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Override
    /*
    this implementation throws an error if a single file in the zipped input stream cannot be parsed!
     */
    public <S extends Ms2Experiment> CloseableIterator<S> parseIterator(InputStream input, URL source) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(input);
        BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(zipInputStream));
        Path sourcePath;
        try {
            sourcePath = Paths.get(source.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }


        ZipEntry entry;
        while ((entry=zipInputStream.getNextEntry())!=null) {
            if (!entry.isDirectory()) break;
        }

        ZipEntry firstEntry;
        S firstEle;
        GenericParser<Ms2Experiment> firstParser;
        if (entry!=null){
            firstEntry = entry;
            firstParser = msExperimentParser.getParser(new File(firstEntry.getName()));
            firstEle = firstParser.parse(r, sourcePath.resolve(firstEntry.getName()).toUri().toURL());
        } else {
            return new CloseableIterator<S>() {
                @Override
                public void close() throws IOException {

                }

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public S next() {
                    return null;
                }
            };
        }



        return new CloseableIterator<S>() {
            @Override
            public void close() throws IOException {
                tryclose();
            }

            BufferedReader reader=r;
            ZipEntry currentEntry = firstEntry;
            GenericParser<Ms2Experiment> parser = firstParser;
            S elem = firstEle;


            @Override
            public boolean hasNext() {
                return elem != null;
            }

            @Override
            public S next() {
                S mem = elem;
                try {
                    elem = parser.parse(reader, source);

                    if (elem==null){
                        currentEntry = nextFile(zipInputStream);
                        if (currentEntry==null){
                            //the end
                            tryclose();
                        } else {
                            parser = msExperimentParser.getParser(new File(currentEntry.getName()));
                            elem = parser.parse(reader, source);
                        }
                    }
                } catch (IOException e){
                    tryclose();
                    throw new RuntimeException(e);
                }
                return mem;
            }

            private ZipEntry nextFile(ZipInputStream zipInputStream) throws IOException {
                ZipEntry entry;
                while ((entry=zipInputStream.getNextEntry())!=null) {
                    if (!entry.isDirectory()) break;
                }
                return entry;
            }

            private void tryclose() {
                try {
                    if (reader != null) {
                        reader.close();
                        reader=null;
                    }
                } catch (IOException e) {

                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    @Override
    public <S extends Ms2Experiment> CloseableIterator<S> parseFromFileIterator(File file) throws IOException {
        final InputStream input = new FileInputStream(file);
        return parseIterator(input, file.toURI().toURL());
    }

    @Override
    public <S extends Ms2Experiment> S parse(BufferedReader reader, URL source) throws IOException {
        String file = source.getFile();
        if (file.endsWith(".zip")){
            file = file.substring(0, file.length()-4);
        }
        GenericParser<Ms2Experiment> parser = msExperimentParser.getParser(new File(file));
        return parser.parse(reader, source);
    }

    /**
     * not supported for zipped files
     * @param input
     * @param <S>
     * @return
     * @throws IOException
     */
    @Override
    public <S extends Ms2Experiment> S parse(InputStream input) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    /**
     * not supported for zipped files
     * @param r
     * @param source
     * @param <S>
     * @return
     * @throws IOException
     */
    @Override
    public <S extends Ms2Experiment> CloseableIterator<S> parseIterator(BufferedReader r, URL source) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public <S extends Ms2Experiment> S parse(BufferedReader reader) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public <S extends Ms2Experiment> CloseableIterator<S> parseIterator(InputStream input) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public <S extends Ms2Experiment> CloseableIterator<S> parseIterator(BufferedReader input) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public <S extends Ms2Experiment> S parseFile(File file) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }
}
