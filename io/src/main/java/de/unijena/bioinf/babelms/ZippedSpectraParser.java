/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.*;
import java.net.URI;
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
    public List<Ms2Experiment> parseFromFile(File file) throws IOException {
        BufferedReader reader = null;
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        final ArrayList<Ms2Experiment> list = new ArrayList<>();
        ZipEntry entry = null;

        try {
            while(entries.hasMoreElements()){
                entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                File asFile = new File(entry.getName());
                final GenericParser<Ms2Experiment> genericParser = msExperimentParser.getParser(asFile);
                InputStream stream = zipFile.getInputStream(entry);
                reader = FileUtils.ensureBuffering(new InputStreamReader(stream));
                final URI source = file.toPath().resolve(entry.getName()).toUri();

                Ms2Experiment elem = genericParser.parse(reader,source);
                while (elem!=null) {
                    list.add(elem);
                    elem = genericParser.parse(reader,source);
                }
                reader.close();
            }
            return list;
        } catch (IOException e) {
            throw new IOException("Error while parsing " + entry.getName() + " in zip archive " + file.getName(), e);
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Override
    /*
    this implementation throws an error if a single file in the zipped input stream cannot be parsed!
     */
    public CloseableIterator<Ms2Experiment> parseIterator(InputStream input, URI source) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(input);
        BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(zipInputStream));
        Path sourcePath = Paths.get(source);

        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) break;
        }

        ZipEntry firstEntry;
        Ms2Experiment firstEle;
        GenericParser<Ms2Experiment> firstParser;
        if (entry!=null){
            firstEntry = entry;
            firstParser = msExperimentParser.getParser(new File(firstEntry.getName()));
            firstEle = firstParser.parse(r, sourcePath.resolve(firstEntry.getName()).toUri());
        } else {
            return new CloseableIterator<>() {
                @Override
                public void close() {}

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Ms2Experiment next() {
                    return null;
                }
            };
        }



        return new CloseableIterator<>() {
            @Override
            public void close() {
                tryclose();
            }

            BufferedReader reader = r;
            ZipEntry currentEntry = firstEntry;
            GenericParser<Ms2Experiment> parser = firstParser;
            Ms2Experiment elem = firstEle;


            @Override
            public boolean hasNext() {
                return elem != null;
            }

            @Override
            public Ms2Experiment next() {
                Ms2Experiment mem = elem;
                try {
                    elem = parser.parse(reader, source);

                    if (elem == null) {
                        currentEntry = nextFile(zipInputStream);
                        if (currentEntry == null) {
                            //the end
                            tryclose();
                        } else {
                            parser = msExperimentParser.getParser(new File(currentEntry.getName()));
                            elem = parser.parse(reader, source);
                        }
                    }
                } catch (IOException e) {
                    tryclose();
                    throw new RuntimeException(e);
                }
                return mem;
            }

            private ZipEntry nextFile(ZipInputStream zipInputStream) throws IOException {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) break;
                }
                return entry;
            }

            private void tryclose() {
                try {
                    if (reader != null) {
                        reader.close();
                        reader = null;
                    }
                } catch (IOException ignored) {}
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    @Override
    public CloseableIterator<Ms2Experiment> parseFromFileIterator(File file) throws IOException {
        final InputStream input = new FileInputStream(file);
        return parseIterator(input, file.toURI());
    }

    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        String file = source.getPath();
        if (file.endsWith(".zip")){
            file = file.substring(0, file.length()-4);
        }
        GenericParser<Ms2Experiment> parser = msExperimentParser.getParser(new File(file));
        return parser.parse(reader, source);
    }

    /**
     * not supported for zipped files
     */
    @Override
    public Ms2Experiment parse(InputStream input) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    /**
     * not supported for zipped files
     */
    @Override
    public CloseableIterator<Ms2Experiment> parseIterator(BufferedReader r, URI source) {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public Ms2Experiment parse(BufferedReader reader) throws IOException {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public CloseableIterator<Ms2Experiment> parseIterator(InputStream input) {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public CloseableIterator<Ms2Experiment> parseIterator(BufferedReader input) {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }

    @Override
    public Ms2Experiment parseFile(File file) {
        throw new UnsupportedOperationException("this method is not supported for zipped files");
    }
}
