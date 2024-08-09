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
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.Ionization;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MassAnalyzer;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@Slf4j
class MzXMLSaxParser extends DefaultHandler {

    private final List<Handler> stack;
    private Handler handler;
    private final StringBuilder buffer;

    private final LCMSRun run;

    private final String sourcePath;

    private final LCMSStorage storage;

    private final DoubleArrayList retentionTimes = new DoubleArrayList();

    private final IntArrayList scanids = new IntArrayList();

    private final Int2IntMap idmap = new Int2IntOpenHashMap();

    private boolean centroided = false;

    private final LCMSParser.IOThrowingConsumer<Scan> scanConsumer;
    private final LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer;

    private final DatatypeFactory datatypeFactory;

    private final ByteList totalBuffer = new ByteArrayList(1024);
    private final byte[] bytebuffer;

    private final Int2LongMap ms1Ids = new Int2LongArrayMap();

    private int samplePolarity = 0;

    public MzXMLSaxParser(
            String sourcePath,
            LCMSStorage storage,
            LCMSRun run,
            @Nullable LCMSParser.IOThrowingConsumer<Scan> scanConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer
    ) throws IOException {
        this.sourcePath = sourcePath;
        this.storage = storage;
        this.run = run;
        this.scanConsumer = scanConsumer;
        this.msmsScanConsumer = msmsScanConsumer;

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
    public void characters(char[] ch, int start, int length) {
        if (handler.listening) {
            buffer.append(ch, start,length);
        }
    }

    public ProcessedSample getProcessedSample() {
        if (scanids.isEmpty()) {
            throw new RuntimeException("No spectra imported from " + sourcePath);
        }

        final ScanPointMapping mapping = new ScanPointMapping(retentionTimes.toDoubleArray(), scanids.toIntArray(), null, idmap);
        storage.setMapping(mapping);
        ProcessedSample sample = new ProcessedSample(mapping, storage, samplePolarity, -1);
        sample.setRun(run);
        return sample;
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
        public void enterElement(String elementName, Attributes attrs) {
            switch (elementName) {
                case "msInstrument":
                    push(new MsInstrumentHandler());
                    break;
                case "dataProcessing":
                    if (!isTrue(attrs,"centroided")) {
                        log.warn("Spectra in file " + sourcePath + "are possibly not centroided! Please check the input data.");
                    } else {
                        centroided = true;
                    }
                    break;
                case "scan":
                    push(new ScanHandler(attrs));
                    break;
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {}

    }

    public class MsInstrumentHandler extends Handler {

        private final List<MassAnalyzer> massAnalyzers = new ArrayList<>();

        @Override
        public void enterElement(String elementName, Attributes attrs) {
            switch (elementName) {
                case "msIonisation":
                    Ionization.byValue(attrs.getValue("value")).ifPresent(run::setIonization);
                    break;
                case "msMassAnalyzer":
                    MassAnalyzer.byValue(attrs.getValue("value")).ifPresent(massAnalyzers::add);
                    break;
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {
            if (elementName.equals("msInstrument")) {
                run.setMassAnalyzers(!massAnalyzers.isEmpty() ? massAnalyzers : null);
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

        Polarity polarity;

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
            polarity = switch (get(attr, "polarity", "any")) {
                case "+" -> Polarity.POSITIVE;
                case "-" -> Polarity.NEGATIVE;
                default -> Polarity.UNKNOWN;
            };
            if (samplePolarity == 0)  {
                samplePolarity = polarity.charge;
            } else if (polarity.charge != 0 && (polarity.charge > 0) != (samplePolarity > 0) ) {
                throw new RuntimeException("Preprocessing does not support LCMS runs with different polarities.");
            }
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
            switch (elementName) {
                case "precursorMz" -> {
                    precursorMz = Double.parseDouble(content);
                    listen(false);
                }
                case "peaks" -> {
                    ByteBuffer W = ByteBuffer.wrap(zlib ? decompress(content) : Base64.getDecoder().decode(content));
                    W.order(ByteOrder.BIG_ENDIAN);
                    if (precision == 0) {
                        // estimate precision from length...
                        if (W.capacity() == (npeaks * 16))
                            precision = 2;
                        else
                            precision = 1;
                    }
                    if (precision == 1) {
                        FloatBuffer f = W.asFloatBuffer();
                        this.mzArray = new double[f.limit() >> 1];
                        this.intensityArray = new double[f.limit() >> 1];
                        for (int i = 0; i < mzArray.length; ++i) {
                            mzArray[i] = f.get();
                            intensityArray[i] = f.get();
                        }
                    } else {
                        DoubleBuffer f = W.asDoubleBuffer();
                        this.mzArray = new double[f.limit() >> 1];
                        this.intensityArray = new double[f.limit() >> 1];
                        for (int i = 0; i < mzArray.length; ++i) {
                            mzArray[i] = f.get();
                            intensityArray[i] = f.get();
                        }
                    }
                }
                case "scan" -> {
                    if (mzArray == null || intensityArray == null || mzArray.length != intensityArray.length || mzArray.length == 0) {
                        log.info("No spectrum data found in Spectrum with number: " + scanNumber + " Skipping!");
                    } else {

                        final SimpleSpectrum peaks = Spectrums.getBaselined(Spectrums.wrap(mzArray, intensityArray), 0);

                        if (peaks.isEmpty()) {
                            log.info("No valid spectrum data found in Spectrum with number: " + scanNumber + " Skipping!");
                        } else {
                            if (msLevel == 1) {
                                if (scanConsumer != null) {
                                    Scan scan = Scan.builder()
                                            .runId(run.getRunId())
                                            .sourceScanId(Integer.toString(scanNumber))
                                            .scanTime(retentionTime)
                                            .peaks(peaks)
                                            .build();
                                    scanConsumer.consume(scan);
                                    ms1Ids.put(scanNumber, scan.getScanId());
                                }

                                final Ms1SpectrumHeader header = new Ms1SpectrumHeader(scanids.size(),scanNumber, Integer.toString(scanNumber), polarity.charge, centroided);
                                retentionTimes.add(retentionTime);
                                idmap.put(scanNumber, scanids.size());
                                scanids.add(scanNumber);
                                storage.getSpectrumStorage().addSpectrum(header, peaks);
                            } else {
                                if (scanConsumer != null && msmsScanConsumer != null) {
                                    MSMSScan scan = MSMSScan.builder()
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

                                final Ms2SpectrumHeader header = new Ms2SpectrumHeader(
                                        Integer.toString(scanNumber),
                                        scanNumber,
                                        polarity.charge, msLevel, centroided,
                                        collisionEnergy,
                                        new IsolationWindow(0d, isolationWindowWidth),
                                        idmap.getOrDefault(precursorScanNumber, -1), // TODO: potential error
                                        precursorMz,
                                        precursorMz,
                                        retentionTime
                                );
                                storage.getSpectrumStorage().addMs2Spectrum(header, peaks);
                            }
                        }
                    }
                    listen(false);
                    pop();
                }
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
