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

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.persistence.model.core.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

class MzXMLSaxParser extends DefaultHandler {

    private final List<Handler> stack;
    private Handler handler;
    private final StringBuilder buffer;

    private final Run.RunBuilder runBuilder;

    private Run run;

    private final String sourcePath;

    private final IOThrowingConsumer<Run> runConsumer;
    private final IOThrowingConsumer<Scan> scanConsumer;
    private final IOThrowingConsumer<MSMSScan> msmsScanConsumer;

    private final MzXMLParser parent;

    private final DatatypeFactory datatypeFactory;

    private final ByteList totalBuffer = new ByteArrayList(1024);
    private final byte[] bytebuffer;

    private final Int2LongMap ms1Ids = new Int2LongArrayMap();

    public MzXMLSaxParser(
            String sourcePath,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder defaultRun,
            MzXMLParser parent
    ) throws IOException {
        this.runBuilder = defaultRun.runId(-1L).sourcePath(sourcePath);
        this.sourcePath = sourcePath;
        this.runConsumer = runConsumer;
        this.scanConsumer = scanConsumer;
        this.msmsScanConsumer = msmsScanConsumer;

        this.parent = parent;

        this.stack = new ArrayList<>();
        this.handler = new RootHandler();

        this.buffer = new StringBuilder();
        this.bytebuffer = new byte[1024];
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            handler.enterElement(qName, attributes);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            if (handler.listening) handler.leaveElement(qName, buffer.toString());
            else handler.leaveElement(qName, "");
            buffer.delete(0, buffer.length());
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (handler.listening) {
            buffer.append(ch, start,length);
        }
    }



    public abstract class Handler {

        protected boolean listening;

        public Handler() {
            this.listening = false;
        }

        public void listen() {
            this.listening = true;
        }

        public void listen(boolean value){
            this.listening = value;
        }

        protected boolean isTrue(Attributes attrs, String name) {
            final String val = attrs.getValue(name);
            return val != null && !val.equals("false") && !val.equals("0");
        }

        protected <T> T get(Attributes a, String name, T defaultValue, Function<String, T> f) {
            String val = a.getValue(name);
            if (val == null) return defaultValue;
            else return f.apply(val);
        }

        protected String get(Attributes a, String name, String defaultValue) {
            String val = a.getValue(name);
            if (val == null || val.isBlank()) return defaultValue;
            else return val;
        }

        public abstract void enterElement(String elementName, Attributes attrs) throws IOException;

        public abstract void leaveElement(String elementName, String content) throws IOException;

        public void push(Handler h) {
            stack.add(this);
            handler = h;
        }

        public void pop() {
            handler = stack.remove(stack.size()-1);
        }
    }

    public class RootHandler extends Handler {

        @Override
        public void enterElement(String elementName, Attributes attrs) throws IOException {
            switch (elementName) {
                case "msInstrument":
                    push(new MsInstrumentHandler());
                    break;
                case "dataProcessing":
                    if (!isTrue(attrs,"centroided")) {
                        LoggerFactory.getLogger(getClass()).debug("Spectra in file " + sourcePath + "are possibly not centroided! Please check the input data.");
                    }
                    break;
                case "scan":
                    push(new ScanHandler(attrs));
                    break;
                case "parentFile":
                    String parentName = Path.of(get(attrs, "fileName", sourcePath)).getFileName().toString();
                    if (parentName.contains(".")) {
                        String[] components = parentName.split("\\.");
                        parentName = Arrays.stream(components).limit(components.length - 1).collect(Collectors.joining());
                    }
                    runBuilder.name(parentName);
                    break;
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {

        }

    }

    public class MsInstrumentHandler extends Handler {

        private final List<MassAnalyzerType> massAnalyzers = new ArrayList<>();

        @Override
        public void enterElement(String elementName, Attributes attrs) {
            switch (elementName) {
                case "msIonisation":
                    Optional<IonizationType> optIonization = parent.matchIonizationType(attrs.getValue("value"));
                    optIonization.ifPresent(runBuilder::ionization);
                    break;
                case "msMassAnalyzer":
                    Optional<MassAnalyzerType> optMassAnalyzer = parent.matchMassAnalyzerType(attrs.getValue("value"));
                    optMassAnalyzer.ifPresent(massAnalyzers::add);
                    break;
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {
            if (elementName.equals("msInstrument")) {
                runBuilder.massAnalyzers(!massAnalyzers.isEmpty() ? massAnalyzers : null);
                pop();
            }
        }
    }

    public class ScanHandler extends Handler {
        double retentionTime;
        CollisionEnergy collisionEnergy ;
        byte msLevel;
        int npeaks;
        int scanNumber;

        int precursorScanNumber;
        double isolationWindowWidth, precursorMz;

        double[] mzArray, intensityArray;

        boolean zlib;

        int precision; // 0=unknown, 1=32, 2=64

        public ScanHandler(Attributes attr) {
            retentionTime = get(attr, "retentionTime", 0d, (s) -> datatypeFactory.newDuration(s).getTimeInMillis(Calendar.getInstance()) / 1000d);
            collisionEnergy = get(attr,"collisionEnergy", CollisionEnergy.none(), x -> new CollisionEnergy(Double.parseDouble(x)));
            msLevel = get(attr, "msLevel", (byte) 1, Byte::parseByte);
            npeaks = get(attr,"peaksCount",0, Integer::parseInt);
            scanNumber = get(attr, "num", -1, Integer::parseInt);
        }

        @Override
        public void enterElement(String elementName, Attributes attrs) {
            switch (elementName) {
                case "precursorMz" -> {
                    precursorScanNumber = get(attrs, "precursorScanNum", -1, Integer::parseInt);
                    isolationWindowWidth = get(attrs, "windowWideness", Double.NaN, Double::parseDouble);
                    listen();
                }
                case "peaks" -> {
                    zlib = get(attrs, "compressionType", false, (x) -> x.equals("zlib"));
                    precision = get(attrs, "precision", 0, (x) -> x.equals("32") ? 1 : (x.equals("64") ? 2 : 0));
                    listen();
                }
                case "scan" -> push(new ScanHandler(attrs));
            }
        }

        @Override
        public void leaveElement(String elementName, String content) throws IOException {
            if (elementName.equals("precursorMz")) {
                precursorMz = Double.parseDouble(content);
                listen(false);
            } else if (elementName.equals("peaks")) {
                ByteBuffer W = ByteBuffer.wrap(zlib ? decompress(content) : Base64.getDecoder().decode(content));
                W.order(ByteOrder.BIG_ENDIAN);
                if (precision==0) {
                    // estimate precision from length...
                    if (W.capacity() == (npeaks*16))
                        precision = 2;
                    else
                        precision = 1;
                }
                if (precision==1) {
                    FloatBuffer f = W.asFloatBuffer();
                    this.mzArray = new double[f.limit()>>1];
                    this.intensityArray = new double[f.limit()>>1];
                    for (int i=0; i < mzArray.length; ++i) {
                        mzArray[i] = f.get();
                        intensityArray[i] = f.get();
                    }
                } else {
                    DoubleBuffer f = W.asDoubleBuffer();
                    this.mzArray = new double[f.limit()>>1];
                    this.intensityArray = new double[f.limit()>>1];
                    for (int i=0; i < mzArray.length; ++i) {
                        mzArray[i] = f.get();
                        intensityArray[i] = f.get();
                    }
                }
            } else if (elementName.equals("scan")) {
                if (mzArray == null || intensityArray == null || mzArray.length != intensityArray.length || mzArray.length == 0) {
                    LoggerFactory.getLogger(MzXMLParser.class).warn("No spectrum data found in Spectrum with number: " + scanNumber + " Skipping!");
                } else {

                    final SimpleSpectrum peaks = Spectrums.getBaselined(Spectrums.wrap(mzArray, intensityArray), 0);

                    if (peaks.isEmpty()) {
                        LoggerFactory.getLogger(MzXMLParser.class).warn("No valid spectrum data found in Spectrum with number: " + scanNumber + " Skipping!");
                    } else {

                        if (run == null) {
                            run = runBuilder.build();
                            runConsumer.consume(run);
                        }

                        if (msLevel == 1) {
                            Scan scan = Scan.builder()
                                    .scanId(-1L)
                                    .runId(run.getRunId())
                                    .scanNumber(Integer.toString(scanNumber))
                                    .scanTime(retentionTime)
                                    .peaks(peaks)
                                    .build();
                            scanConsumer.consume(scan);
                            ms1Ids.put(scanNumber, scan.getScanId());
                        } else {
                            MSMSScan scan = MSMSScan.builder()
                                    .scanId(-1L)
                                    .runId(run.getRunId())
                                    .scanNumber(Integer.toString(scanNumber))
                                    .scanTime(retentionTime)
                                    .peaks(peaks)
                                    .msLevel(msLevel)
                                    .collisionEnergy(collisionEnergy)
                                    .mzOfInterest(precursorMz)
                                    .isolationWindow(new IsolationWindow(0d, isolationWindowWidth))
                                    .precursorScanId(ms1Ids.getOrDefault(precursorScanNumber, -1L))
                                    .build();
                            msmsScanConsumer.consume(scan);
                        }
                    }
                }
                listen(false);
                pop();
            }
        }

        private byte[] decompress(String content) {
            totalBuffer.clear();
            final Inflater inflater = new Inflater();
            inflater.setInput(Base64.getDecoder().decode(content));
            while (true){
                try {
                    int written = inflater.inflate(bytebuffer);
                    if (written == 0) return totalBuffer.toArray(new byte[0]);
                    else totalBuffer.addElements(totalBuffer.size(), bytebuffer, 0, written);
                } catch (DataFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

}
